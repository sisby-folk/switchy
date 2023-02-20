package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyModules;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static folk.sisby.switchy.util.Feedback.getIdText;

public class SwitchyPresets {

	public final Map<String, SwitchyPreset> presets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public final Map<Identifier, Boolean> modules;
	public SwitchyPreset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ENABLED = "enabled";
	public static final String KEY_PRESET_MODULE_DISABLED = "disabled";
	public static final String KEY_PRESET_LIST = "list";

	private SwitchyPresets() {
		modules = SwitchyModules.MODULE_SUPPLIERS.entrySet().stream()
				.filter(e -> e.getValue().get() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> SwitchyModules.MODULE_INFO.get(e.getKey()).isDefault()));
	}

	public static SwitchyPresets fromNbt(NbtCompound nbt, boolean forPlayer) {
		SwitchyPresets outPresets = new SwitchyPresets();

		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE), true, !forPlayer);
		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE), false, !forPlayer);

		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			SwitchyPreset preset = SwitchyPreset.fromNbt(key, listNbt.getCompound(key), outPresets.modules);
			try {
				outPresets.addPreset(preset);
			} catch (IllegalStateException ignored) {
				Switchy.LOGGER.warn("Switchy: Player data contained duplicate preset '{}'. Data may have been lost.", preset.presetName);
			}
		}

		if (forPlayer) {
			if (nbt.contains(KEY_PRESET_CURRENT))
				try {
					outPresets.setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT));
				} catch (IllegalArgumentException ignored) {
					Switchy.LOGGER.warn("Switchy: Unable to set current preset from data. Data may have been lost.");
				}

			if (outPresets.presets.isEmpty() || outPresets.getCurrentPreset() == null) {
				// Recover current data as "Default" preset
				outPresets.addPreset(new SwitchyPreset("default", outPresets.modules));
				outPresets.setCurrentPreset("default");
			}
		}

		return outPresets;
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		modules.forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound listNbt = new NbtCompound();
		for (SwitchyPreset preset : presets.values()) {
			listNbt.put(preset.presetName, preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		if (currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.presetName);
		return outNbt;
	}

	public void importFromOther(@Nullable ServerPlayerEntity player, SwitchyPresets other) {
		importFromOther(player, other.presets);
	}

	public void importFromOther(@Nullable ServerPlayerEntity player, Map<String, SwitchyPreset> other) {
		// Replace enabled modules for colliding current preset
		if (other.containsKey(this.currentPreset.presetName) && player != null) {
			other.get(this.currentPreset.presetName).modules.forEach((moduleId, module) -> {
				duckCurrentModule(player, moduleId, (duckedModule) -> {
					duckedModule.fillFromNbt(duckedModule.toNbt());
				});
			});
		}
		other.remove(currentPreset.presetName);

		// Replace enabled modules for collisions
		other.entrySet().stream().filter(e -> presets.containsKey(e.getKey())).forEach(e -> e.getValue().modules.forEach((moduleId, module) -> {
			presets.get(e.getKey()).modules.remove(moduleId);
			presets.get(e.getKey()).modules.put(moduleId, module);
		}));

		// Add non-colliding presets
		other.forEach((name, preset) -> {
			if (!presets.containsKey(name)) {
				modules.forEach((moduleId, enabled) -> {
					if (enabled && !preset.modules.containsKey(moduleId)) { // Add missing modules
						preset.modules.put(moduleId, SwitchyModules.MODULE_SUPPLIERS.get(moduleId).get());
					}
				});
				addPreset(preset);
			}
		});
	}

	private void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		list.forEach((e) -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null && modules.containsKey(id)) {
				modules.put(id, enabled);
			} else if (!silent) {
				Switchy.LOGGER.warn("Switchy: Unable to toggle a module - Was a module unloaded?");
				Switchy.LOGGER.warn("Switchy: NBT Element: " + e.asString());
			}
		});
	}

	private void setCurrentPreset(String presetName) throws IllegalArgumentException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		currentPreset = presets.get(presetName);
	}

	public String switchCurrentPreset(ServerPlayerEntity player, String presetName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presetName.equalsIgnoreCase(Objects.toString(currentPreset, "")))
			throw new IllegalStateException("Specified preset is already current");

		SwitchyPreset newPreset = presets.get(presetName);

		// Perform Switch
		currentPreset.updateFromPlayer(player, newPreset.presetName);
		newPreset.applyToPlayer(player);

		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				player.getUuid(), newPreset.presetName, Objects.toString(currentPreset, null), getEnabledModuleNames()
		);
		currentPreset = newPreset;
		SwitchyEvents.SWITCH.invoker().onSwitch(player, switchEvent);

		return currentPreset.presetName;
	}

	public void saveCurrentPreset(ServerPlayerEntity player) {
		if (currentPreset != null) currentPreset.updateFromPlayer(player, null);
	}

	public void duckCurrentModule(ServerPlayerEntity player, Identifier moduleId, Consumer<SwitchyModule> mutator) throws IllegalArgumentException, IllegalStateException {
		if (currentPreset == null) throw new IllegalStateException("Specified player has no current preset");
		if (!modules.containsKey(moduleId)) throw new IllegalArgumentException("Specified module does not exist");
		if (!modules.get(moduleId)) throw new IllegalStateException("Specified module is not enabled");
		SwitchyModule module = currentPreset.modules.get(moduleId);
		module.updateFromPlayer(player, null);
		mutator.accept(module);
		module.applyToPlayer(player);
	}

	public void addPreset(SwitchyPreset preset) throws IllegalStateException {
		if (presets.containsKey(preset.presetName))
			throw new IllegalStateException("Specified preset already exists.");
		presets.put(preset.presetName, preset);
	}

	public void deletePreset(String presetName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (currentPreset.presetName.equalsIgnoreCase(presetName))
			throw new IllegalStateException("Specified preset is current");
		presets.remove(presetName);
	}

	public void renamePreset(String oldName, String newName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(oldName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presets.containsKey(newName)) throw new IllegalStateException("Specified preset name already exists");
		SwitchyPreset preset = presets.get(oldName);
		preset.presetName = newName;
		presets.put(newName, preset);
		presets.remove(oldName);
	}

	public void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!modules.get(id)) throw new IllegalStateException("Specified module is already disabled");
		modules.put(id, false);
		presets.forEach((name, preset) -> preset.modules.remove(id));
	}

	public void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (modules.get(id)) throw new IllegalStateException("Specified module is already enabled");
		modules.put(id, true);
		presets.values().forEach(preset -> preset.modules.put(id, SwitchyModules.MODULE_SUPPLIERS.get(id).get()));
	}

	public SwitchyPreset getCurrentPreset() {
		return currentPreset;
	}

	public List<String> getPresetNames() {
		return presets.keySet().stream().sorted().toList();
	}

	public List<Identifier> getEnabledModules() {
		return modules.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
	}

	public List<String> getEnabledModuleNames() {
		return getEnabledModules().stream().map(Identifier::getPath).toList();
	}

	public MutableText getEnabledModuleText() {
		return getIdText(getEnabledModules());
	}

	public List<Identifier> getDisabledModules() {
		return modules.entrySet().stream().filter((e) -> !e.getValue()).map(Map.Entry::getKey).toList();
	}

	@Override
	public String toString() {
		return presets.keySet().toString();
	}
}

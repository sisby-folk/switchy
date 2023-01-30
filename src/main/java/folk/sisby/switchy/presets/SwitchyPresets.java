package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchySwitchEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static folk.sisby.switchy.Switchy.S2C_SWITCH;
import static folk.sisby.switchy.util.Feedback.getIdText;

public class SwitchyPresets {

	private final Map<String, SwitchyPreset> presetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public final Map<Identifier, Boolean> modules;
	private SwitchyPreset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ENABLED = "enabled";
	public static final String KEY_PRESET_MODULE_DISABLED = "disabled";
	public static final String KEY_PRESET_LIST = "list";

	private SwitchyPresets() {
		modules = Switchy.MODULE_SUPPLIERS.entrySet().stream()
				.filter(e -> e.getValue().get() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Switchy.MODULE_INFO.get(e.getKey()).isDefault()));
	}

	public static SwitchyPresets fromNbt(NbtCompound nbt, @Nullable PlayerEntity player) {
		SwitchyPresets outPresets = new SwitchyPresets();

		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE), true, player == null);
		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE), false, player == null);

		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			SwitchyPreset preset = SwitchyPreset.fromNbt(key, listNbt.getCompound(key), outPresets.modules);
			try {
				outPresets.addPreset(preset);
			} catch (IllegalStateException ignored){
				Switchy.LOGGER.warn("Switchy: Player data contained duplicate preset '{}'. Data may have been lost.", preset.presetName);
			}
		}

		if (player != null) {
			if (nbt.contains(KEY_PRESET_CURRENT))
				try {
					outPresets.setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT));
				} catch (IllegalArgumentException ignored) {
					Switchy.LOGGER.warn("Switchy: Unable to set current preset from data. Data may have been lost.");
				}

			if (outPresets.presetMap.isEmpty() || outPresets.getCurrentPreset() == null) {
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
		for (SwitchyPreset preset : presetMap.values()) {
			listNbt.put(preset.presetName, preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		if (currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.presetName);
		return outNbt;
	}

	public void importFromOther(SwitchyPresets other) throws IllegalStateException {
		if (other.getPresetNames().stream().anyMatch((name) -> getPresetNames().contains(name))) throw new IllegalStateException("Specified preset already exists");

		// Re-enable missing empty modules
		modules.forEach((key, enabled) -> {
			if (enabled && !other.modules.get(key)) {
				other.enableModule(key);
			}
		});

		other.presetMap.values().forEach(this::addPreset);
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
		if (!presetMap.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		currentPreset = presetMap.get(presetName);
	}

	public String switchCurrentPreset(ServerPlayerEntity player, String presetName) throws IllegalArgumentException, IllegalStateException {
		if (!presetMap.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presetName.equalsIgnoreCase(Objects.toString(currentPreset, ""))) throw new IllegalStateException("Specified preset is already current");

		SwitchyPreset newPreset = presetMap.get(presetName);

		// Perform Switch
		currentPreset.updateFromPlayer(player, newPreset.presetName);
		newPreset.applyToPlayer(player);

		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				player.getUuid(), newPreset.presetName, Objects.toString(currentPreset, null), getEnabledModuleNames()
		);
		currentPreset = newPreset;

		// Fire Events
		SwitchyEvents.fireSwitch(switchEvent);
		if (ServerPlayNetworking.canSend(player, S2C_SWITCH)) {
			ServerPlayNetworking.send(player, S2C_SWITCH, PacketByteBufs.create().writeNbt(switchEvent.toNbt()));
		}

		return currentPreset.presetName;
	}

	public void saveCurrentPreset(PlayerEntity player) {
		if (currentPreset != null) currentPreset.updateFromPlayer(player, null);
	}

	public void addPreset(SwitchyPreset preset) throws IllegalStateException {
		if (presetMap.containsKey(preset.presetName)) throw new IllegalStateException("Specified preset already exists.");
		presetMap.put(preset.presetName, preset);
	}

	public void deletePreset(String presetName) throws IllegalArgumentException, IllegalStateException {
		if (!presetMap.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (currentPreset.presetName.equalsIgnoreCase(presetName)) throw new IllegalStateException("Specified preset is current");
		presetMap.remove(presetName);
	}

	public void renamePreset(String oldName, String newName) throws IllegalArgumentException, IllegalStateException {
		if (!presetMap.containsKey(oldName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presetMap.containsKey(newName)) throw new IllegalStateException("Specified preset name already exists");
		SwitchyPreset preset = presetMap.get(oldName);
		preset.presetName = newName;
		presetMap.put(newName, preset);
		presetMap.remove(oldName);
	}

	public void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException  {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!modules.get(id)) throw new IllegalStateException("Specified module is already disabled");
		modules.put(id, false);
		presetMap.forEach((name, preset) -> preset.compatModules.remove(id));
	}

	public void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (modules.get(id)) throw new IllegalStateException("Specified module is already enabled");
		modules.put(id, true);
		presetMap.values().forEach(preset -> preset.compatModules.put(id, Switchy.MODULE_SUPPLIERS.get(id).get()));
	}

	public SwitchyPreset getCurrentPreset() {
		return currentPreset;
	}

	public List<String> getPresetNames() {
		return presetMap.keySet().stream().sorted().toList();
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
		return presetMap.keySet().toString();
	}
}

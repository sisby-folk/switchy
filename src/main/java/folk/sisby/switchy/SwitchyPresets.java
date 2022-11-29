package folk.sisby.switchy;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static folk.sisby.switchy.util.Feedback.literal;

public class SwitchyPresets {

	private final Map<String, SwitchyPreset> presetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public final Map<Identifier, Boolean> modules;
	private SwitchyPreset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ENABLED = "enabled";
	public static final String KEY_PRESET_MODULE_DISABLED = "disabled";
	public static final String KEY_PRESET_LIST = "list";

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		this.modules.forEach((key, value) -> {
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

		if (this.currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.presetName);
		return outNbt;
	}

	public static SwitchyPresets fromNbt(NbtCompound nbt, @Nullable PlayerEntity player) {
		SwitchyPresets outPresets = new SwitchyPresets();

		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE), true, player == null);
		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE), false, player == null);

		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			SwitchyPreset preset = SwitchyPreset.fromNbt(key, listNbt.getCompound(key), outPresets.modules);
			if (!outPresets.addPreset(preset)) {
				Switchy.LOGGER.warn("Switchy: Player data contained duplicate preset. Data may have been lost.");
			}
		}

		if (player != null) {
			if (nbt.contains(KEY_PRESET_CURRENT) && !outPresets.setCurrentPreset(player, nbt.getString(KEY_PRESET_CURRENT), false)) {
				Switchy.LOGGER.warn("Switchy: Unable to set current preset from data. Data may have been lost.");
			}

			if (outPresets.presetMap.isEmpty() || outPresets.getCurrentPreset() == null) {
				// Recover current data as "Default" preset
				outPresets.addPreset(new SwitchyPreset("default", outPresets.modules));
				outPresets.setCurrentPreset(player, "default", false);
			}
		}

		return outPresets;
	}

	public boolean importFromOther(SwitchyPresets other, List<Identifier> importModules) {
		if (other.getPresetNames().stream().noneMatch((name) -> this.getPresetNames().contains(name))) {
			// Remove modules that shouldn't be imported
			this.modules.forEach((key, enabled) -> {
				if (other.modules.get(key) && (!importModules.contains(key) || !enabled)) {
					other.disableModule(key);
				}
			});

			// Re-enable missing empty modules
			this.modules.forEach((key, enabled) -> {
				if (enabled && !other.modules.get(key)) {
					other.enableModule(key);
				}
			});

			other.presetMap.values().forEach(this::addPreset);
			return true;
		} else {
			return false;
		}
	}

	private void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		list.forEach((e) -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null && this.modules.containsKey(id)) {
				this.modules.put(id, enabled);
			} else if (!silent) {
				Switchy.LOGGER.warn("Switchy: Unable to toggle a module - Was a module unloaded?");
				Switchy.LOGGER.warn("Switchy: NBT Element: " + e.asString());
			}
		});
	}

	private SwitchyPresets() {
		this.modules = Switchy.COMPAT_REGISTRY.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get().isDefault()));
	}

	public boolean setCurrentPreset(PlayerEntity player, String presetName, Boolean performSwitch) {
		if (this.presetMap.containsKey(presetName)) {
			SwitchyPreset newPreset = this.presetMap.get(presetName);
			if (performSwitch) this.switchPreset(player, currentPreset, newPreset);
			this.currentPreset = newPreset;
			return true;
		} else {
			return false;
		}
	}

	private void switchPreset(PlayerEntity player, SwitchyPreset oldPreset, SwitchyPreset newPreset) {
		oldPreset.updateFromPlayer(player, newPreset.presetName);
		newPreset.applyToPlayer(player);
	}

	public void saveCurrentPreset(PlayerEntity player) {
		if (this.currentPreset != null) this.currentPreset.updateFromPlayer(player, null);
	}

	public SwitchyPreset getCurrentPreset() {
		return currentPreset;
	}

	public boolean addPreset(SwitchyPreset preset) {
		if (presetMap.containsKey(preset.presetName)) {
			return false;
		} else {
			presetMap.put(preset.presetName, preset);
			return true;
		}
	}

	@Override
	public String toString() {
		return presetMap.keySet().toString();
	}

	public List<String> getPresetNames() {
		return presetMap.keySet().stream().sorted().toList();
	}

	public void deletePreset(String presetName) {
		if (this.presetMap.containsKey(presetName) && !this.currentPreset.toString().equalsIgnoreCase(presetName)) {
			this.presetMap.remove(presetName);
		} else {
			throw new IllegalArgumentException("Switchy can't delete that preset");
		}
	}

	public void renamePreset(String oldName, String newName) {
		if (this.presetMap.containsKey(oldName) && !this.presetMap.containsKey(newName)) {
			SwitchyPreset preset = this.presetMap.get(oldName);
			preset.presetName = newName;
			this.presetMap.put(newName, preset);
			this.presetMap.remove(oldName);
		} else {
			throw new IllegalArgumentException("Switchy rename not valid");
		}
	}

	public boolean containsPreset(String presetName) {
		return this.presetMap.containsKey(presetName); // Case Insensitive
	}

	public void disableModule(Identifier id) {
		if (this.modules.containsKey(id)) {
			this.modules.put(id, false);
			presetMap.forEach((name, preset) -> preset.compatModules.remove(id));
		} else {
			throw new IllegalArgumentException("Switchy module doesn't exist");
		}
	}

	public void enableModule(Identifier id) {
		if (this.modules.containsKey(id)) {
			this.modules.put(id, true);
			presetMap.forEach((name, preset) -> preset.compatModules.put(id, Switchy.COMPAT_REGISTRY.get(id).get()));
		} else {
			throw new IllegalArgumentException("Switchy module doesn't exist");
		}
	}

	public List<Identifier> getEnabledModules() {
		return this.modules.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
	}

	public List<String> getEnabledModuleNames() {
		return getEnabledModules().stream().map(Identifier::getPath).toList();
	}

	public MutableText getEnabledModuleText() {
		MutableText outText = literal("[");
		List<Identifier> enabledModules = getEnabledModules();
		for (int i = 0; i < enabledModules.size(); i++) {
			outText.append(literal(enabledModules.get(i).getPath()).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, literal(enabledModules.get(i).toString())))));
			if (i < enabledModules.size() - 1) {
				outText.append(", ");
			}
		}
		outText.append(literal("]"));
		return outText;
	}

	public List<Identifier> getDisabledModules() {
		return this.modules.entrySet().stream().filter((e) -> !e.getValue()).map(Map.Entry::getKey).toList();
	}
}

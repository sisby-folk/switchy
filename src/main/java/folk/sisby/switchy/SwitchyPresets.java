package folk.sisby.switchy;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SwitchyPresets {

	private final Map<String, SwitchyPreset> presetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<Identifier, Boolean> moduleToggles;
	private final PlayerEntity player;
	@Nullable private SwitchyPreset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ALLOWED = "allowed";
	public static final String KEY_PRESET_MODULE_DENIED = "denied";
	public static final String KEY_PRESET_LIST = "list";

	public NbtElement toNbt() {
		NbtCompound outNbt = new NbtCompound();

		NbtList allowedList = new NbtList();
		NbtList deniedList = new NbtList();

		this.moduleToggles.forEach((key, value) -> {
			if (value) allowedList.add(NbtString.of(key.toString()));
			if (!value) deniedList.add(NbtString.of(key.toString()));
		});

		outNbt.put(KEY_PRESET_MODULE_ALLOWED, allowedList);
		outNbt.put(KEY_PRESET_MODULE_DENIED, deniedList);

		NbtCompound listNbt = new NbtCompound();
		for (SwitchyPreset preset : presetMap.values()) {
			listNbt.put(preset.presetName, preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		if (this.currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.presetName);
		return outNbt;
	}

	public static SwitchyPresets fromNbt(PlayerEntity player, NbtCompound nbt) {
		SwitchyPresets outPresets = new SwitchyPresets(player);

		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ALLOWED, NbtType.STRING), true);
		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DENIED, NbtType.STRING), false);

		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			SwitchyPreset preset = SwitchyPreset.fromNbt(key, listNbt.getCompound(key), outPresets.moduleToggles);
			if (!outPresets.addPreset(preset)) {
				Switchy.LOGGER.warn("Switchy: Player data contained duplicate preset. Data may have been lost.");
			}
		}

		if (nbt.contains(KEY_PRESET_CURRENT) && !outPresets.setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT), false)) {
			Switchy.LOGGER.warn("Switchy: Unable to set current preset from data. Data may have been lost.");
		}

		return outPresets;
	}

	private void toggleModulesFromNbt(NbtList list, Boolean enabled) {
		list.forEach((e) -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null && this.moduleToggles.containsKey(id)) {
				this.moduleToggles.put(id, enabled);
			} else {
				Switchy.LOGGER.warn("Switchy: Unable to toggle a module - Was a module unloaded?");
			}
		});
	}

	private SwitchyPresets(PlayerEntity player) {
		this.player = player;
		this.moduleToggles = Switchy.COMPAT_REGISTRY.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get().isDefault()));
	}

	public boolean setCurrentPreset(String presetName, Boolean performSwitch) {
		if (this.presetMap.containsKey(presetName)) {
			SwitchyPreset newPreset = this.presetMap.get(presetName);
			if (performSwitch) this.switchPreset(currentPreset, newPreset);
			this.currentPreset = newPreset;
			return true;
		} else {
			return false;
		}
	}

	private void switchPreset(SwitchyPreset oldPreset, SwitchyPreset newPreset) {
		if (oldPreset != null) {
			// Preserve current values into preset before swapping
			oldPreset.updateFromPlayer(player);
		}
		newPreset.applyToPlayer(player);
	}

	public void saveCurrentPreset() {
		if (this.currentPreset != null) this.currentPreset.updateFromPlayer(player);
	}

	public @Nullable SwitchyPreset getCurrentPreset() {
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

	public boolean deletePreset(String presetName) {
		if (this.presetMap.containsKey(presetName)) {
			if (presetName.equalsIgnoreCase(Objects.toString(this.currentPreset, null))) {
				this.currentPreset = null;
			}
			this.presetMap.remove(presetName);
			return true;
		} else {
			return false;
		}
	}

	public boolean renamePreset(String oldName, String newName) {
		if (this.presetMap.containsKey(oldName) && !this.presetMap.containsKey(newName)) {
			SwitchyPreset preset = this.presetMap.get(oldName);
			this.presetMap.put(newName, preset);
			this.presetMap.remove(oldName);
			return true;
		} else {
			return false;
		}
	}

	public boolean denyModule(Identifier id) {
		if (this.moduleToggles.containsKey(id)) {
			this.moduleToggles.put(id, false);
			return true;
		} else {
			return false;
		}
	}

	public boolean allowModule(Identifier id) {
		if (this.moduleToggles.containsKey(id)) {
			this.moduleToggles.put(id, true);
			return true;
		} else {
			return false;
		}
	}

	public Map<Identifier, Boolean> getModuleToggles() {
		return this.moduleToggles;
	}
}

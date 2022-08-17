package folk.sisby.switchy;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class SwitchyPresets {

	private final Map<String, SwitchyPreset> presetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final PlayerEntity player;
	@Nullable private SwitchyPreset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_LIST = "list";

	public NbtElement toNbt() {
		NbtCompound outNbt = new NbtCompound();
		NbtCompound listNbt = new NbtCompound();
		for (SwitchyPreset preset : presetMap.values()) {
			listNbt.put(preset.presetName, preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);
		if (this.currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.presetName);
		return outNbt;
	}

	public static SwitchyPresets fromNbt(PlayerEntity player, NbtCompound nbt) {
		SwitchyPresets outPresets = SwitchyPresets.fromEmpty(player);
		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			SwitchyPreset preset = SwitchyPreset.fromNbt(key, nbt.getCompound(key));
			if (!outPresets.addPreset(preset)) {
				Switchy.LOGGER.warn("Player data contained duplicate preset. Data may have been lost.");
			}
		}
		if (nbt.contains(KEY_PRESET_CURRENT) && !outPresets.setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT))) {
			Switchy.LOGGER.warn("Unable to set current preset from data. Data may have been lost.");
		}
		return outPresets;
	}

	public static SwitchyPresets fromEmpty(PlayerEntity player) {
		return new SwitchyPresets(player);
	}

	private SwitchyPresets(PlayerEntity player) {
		this.player = player;
	}

	public boolean setCurrentPreset(String presetName) {
		if (this.presetMap.containsKey(presetName)) {
			SwitchyPreset newPreset = this.presetMap.get(presetName);
			this.switchPreset(currentPreset, newPreset);
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
}

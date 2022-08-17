package folk.sisby.switchy;

import folk.sisby.switchy.compat.PresetCompat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SwitchyPreset {
	public final List<PresetCompat> compatModules = new ArrayList<>();

	public static final String KEY_PRESET_NAME = "presetName";
	public static final String KEY_PRESET_DATA = "presetData";

	public String presetName;

	public SwitchyPreset(String name) {
		this.presetName = name;
		Switchy.COMPAT_MODULES.forEach((cm) -> this.compatModules.add(cm.getEmptyModule()));
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		outNbt.putString(KEY_PRESET_NAME, Optional.ofNullable(this.presetName).orElse("\u0000"));
		NbtCompound dataNbt = new NbtCompound();
		for (PresetCompat module : compatModules) {
			dataNbt.put(module.getKey(), module.toNbt(this));
		}
		outNbt.put(KEY_PRESET_DATA, dataNbt);
		return outNbt;
	}

	public static SwitchyPreset fromNbt(NbtCompound nbt) {
		SwitchyPreset outPreset = new SwitchyPreset(nbt.getString(KEY_PRESET_NAME));
		NbtCompound dataNbt = nbt.getCompound(KEY_PRESET_NAME);
		for (PresetCompat module : outPreset.compatModules) {
			module.fillFromNbt(dataNbt.getCompound(module.getKey()));
		}
		return outPreset;
	}

	public void updateFromPlayer(PlayerEntity player) {
		for (PresetCompat module : compatModules) {
			module.updateFromPlayer(player);
		}
	}

	public void applyToPlayer(PlayerEntity player) {
		for (PresetCompat module : compatModules) {
			module.applyToPlayer(player);
		}
	}

	@Override
	public String toString() {
		return presetName;
	}
}

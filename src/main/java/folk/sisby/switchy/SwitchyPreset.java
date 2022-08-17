package folk.sisby.switchy;

import folk.sisby.switchy.compat.PresetCompat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class SwitchyPreset {
	public final List<PresetCompat> compatModules = new ArrayList<>();

	public String presetName;

	public SwitchyPreset(String name) {
		this.presetName = name;
		Switchy.COMPAT_MODULES.forEach((cm) -> this.compatModules.add(cm.getEmptyModule()));
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		for (PresetCompat module : compatModules) {
			outNbt.put(module.getKey(), module.toNbt(this));
		}
		return outNbt;
	}

	public static SwitchyPreset fromNbt(String presetName, NbtCompound nbt) {
		SwitchyPreset outPreset = new SwitchyPreset(presetName);
		for (PresetCompat module : outPreset.compatModules) {
			module.fillFromNbt(nbt.getCompound(module.getKey()));
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

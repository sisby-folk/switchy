package folk.sisby.switchy;

import folk.sisby.switchy.compat.PresetCompatModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class SwitchyPreset {
	public final List<PresetCompatModule> compatModules = new ArrayList<>();

	public String presetName;

	public SwitchyPreset(String name) {
		this.presetName = name;
		Switchy.COMPAT_MODULES.forEach((cm) -> this.compatModules.add(cm.get()));
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		for (PresetCompatModule module : compatModules) {
			outNbt.put(module.getKey(), module.toNbt(this));
		}
		return outNbt;
	}

	public static SwitchyPreset fromNbt(String presetName, NbtCompound nbt) {
		SwitchyPreset outPreset = new SwitchyPreset(presetName);
		for (PresetCompatModule module : outPreset.compatModules) {
			module.fillFromNbt(nbt.getCompound(module.getKey()));
		}
		return outPreset;
	}

	public void updateFromPlayer(PlayerEntity player) {
		for (PresetCompatModule module : compatModules) {
			module.updateFromPlayer(player);
		}
	}

	public void applyToPlayer(PlayerEntity player) {
		for (PresetCompatModule module : compatModules) {
			module.applyToPlayer(player);
		}
	}

	@Override
	public String toString() {
		return presetName;
	}
}

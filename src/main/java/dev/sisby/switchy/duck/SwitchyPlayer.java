package dev.sisby.switchy.duck;

import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.nbt.NbtCompound;

public interface SwitchyPlayer {
	void switchy$hotSwap(NbtCompound nbt);

	SwitchyPlayerData switchy$playerData();
}

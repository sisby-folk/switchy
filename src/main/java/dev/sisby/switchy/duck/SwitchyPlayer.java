package dev.sisby.switchy.duck;

import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public interface SwitchyPlayer {
	void switchy$hotSwap(NbtCompound nbt, Text reason);

	SwitchyPlayerData switchy$playerData();
}

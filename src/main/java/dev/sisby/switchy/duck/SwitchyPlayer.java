package dev.sisby.switchy.duck;

import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public interface SwitchyPlayer {
	CompoundTag switchy$hotSwapData();

	void switchy$startReload();

	void switchy$finishReload();

	void switchy$hotSwap(CompoundTag nbt, Component reason);

	SwitchyPlayerData switchy$getOrCreatePlayerData();

	SwitchyPlayerData switchy$getPlayerData();
}

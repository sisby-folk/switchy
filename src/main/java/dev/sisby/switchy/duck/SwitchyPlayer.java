package dev.sisby.switchy.duck;

import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public interface SwitchyPlayer {
	NbtCompound switchy$hotSwapData();

	void switchy$startReload();

	void switchy$finishReload();

	void switchy$hotSwap(NbtCompound nbt, Text reason);

	SwitchyPlayerData switchy$getOrCreatePlayerData();

	SwitchyPlayerData switchy$getPlayerData();

    SwitchyProfile switchy$getSayProfile();

	void switchy$setSayProfile(SwitchyProfile profile);
}

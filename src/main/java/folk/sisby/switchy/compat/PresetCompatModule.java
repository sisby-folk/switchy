package folk.sisby.switchy.compat;

import folk.sisby.switchy.SwitchyPreset;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public abstract class PresetCompatModule {

	public abstract void updateFromPlayer(PlayerEntity player);

	public abstract void applyToPlayer(PlayerEntity player);

	public abstract NbtCompound toNbt(SwitchyPreset preset);

	public abstract void fillFromNbt(NbtCompound nbt);

	public abstract String getKey();
}

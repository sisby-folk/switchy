package folk.sisby.switchy.compat;

import folk.sisby.switchy.SwitchyPreset;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public abstract class PresetCompatModule {

	public abstract void updateFromPlayer(PlayerEntity player);

	public abstract void applyToPlayer(PlayerEntity player);

	public abstract NbtCompound toNbt(SwitchyPreset preset);

	public abstract void fillFromNbt(NbtCompound nbt);

	public abstract Identifier getId();

	public abstract boolean isDefault();
}

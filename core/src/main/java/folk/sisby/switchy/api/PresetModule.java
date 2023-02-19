package folk.sisby.switchy.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

public interface PresetModule {
	void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset);

	void applyToPlayer(PlayerEntity player);

	default NbtCompound toNbt() {return toNbt(false);}

	default NbtCompound toNbt(boolean displayOnly) {
		return displayOnly ? new NbtCompound() : toNbt();
	}

	void fillFromNbt(NbtCompound nbt);
}

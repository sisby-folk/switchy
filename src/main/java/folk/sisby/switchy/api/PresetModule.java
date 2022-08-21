package folk.sisby.switchy.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public interface PresetModule {

	void updateFromPlayer(PlayerEntity player);

	void applyToPlayer(PlayerEntity player);

	NbtCompound toNbt();

	void fillFromNbt(NbtCompound nbt);

	Identifier getId();

	default String getDisableConfirmation() {
		return "Warning: Module data will be immediately lost.";
	}

	boolean isDefault();
}

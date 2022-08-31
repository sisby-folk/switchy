package folk.sisby.switchy.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public interface PresetModule {

	void updateFromPlayer(PlayerEntity player);

	void applyToPlayer(PlayerEntity player);

	NbtCompound toNbt();

	void fillFromNbt(NbtCompound nbt);

	Identifier getId();

	default MutableText getDisableConfirmation() {
		return new TranslatableText("commands.switchy.module.warn.default");
	}

	boolean isDefault();
}

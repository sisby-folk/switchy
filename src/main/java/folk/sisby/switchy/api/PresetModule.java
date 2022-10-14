package folk.sisby.switchy.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;

public interface PresetModule {

	void updateFromPlayer(PlayerEntity player);

	void applyToPlayer(PlayerEntity player);

	NbtCompound toNbt();

	void fillFromNbt(NbtCompound nbt);

	Identifier getId();


	default Collection<Identifier> getApplyDependencies() {
		return Set.of();
	}

	default MutableText getDisableConfirmation() {
		return Text.translatable("commands.switchy.module.disable.warn");
	}

	boolean isDefault();
}

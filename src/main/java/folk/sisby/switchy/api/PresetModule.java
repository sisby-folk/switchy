package folk.sisby.switchy.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

import static folk.sisby.switchy.util.Feedback.translatable;

public interface PresetModule {

	@Deprecated
	default void updateFromPlayer(PlayerEntity player) {
		updateFromPlayer(player, null);
	}

	default void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		updateFromPlayer(player);
	}

	void applyToPlayer(PlayerEntity player);

	NbtCompound toNbt();

	void fillFromNbt(NbtCompound nbt);

	Identifier getId();

	default Collection<Identifier> getApplyDependencies() {
		return Set.of();
	}

	default MutableText getDisableConfirmation() {
		return translatable("commands.switchy.module.disable.warn");
	}

	boolean isDefault();

	default ModuleImportable getImportable() {return ModuleImportable.OPERATOR;}
}

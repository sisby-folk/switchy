package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import virtuoel.pehkui.api.ScaleTypes;

public class PehkuiCompat implements PresetModule {
	private static final Identifier ID = new Identifier("switchy", "pehkui");
	private static final boolean isDefault = true;

	public static final String KEY_SCALE_WIDTH = "scaleWidth";
	public static final String KEY_SCALE_HEIGHT = "scaleHeight";

	// Overwritten on save when null
	@Nullable public Float scaleHeight;
	@Nullable public Float scaleWidth;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		this.scaleHeight = ScaleTypes.HEIGHT.getScaleData(player).getTargetScale();
		this.scaleWidth = ScaleTypes.WIDTH.getScaleData(player).getTargetScale();
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		if (this.scaleHeight != null) ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(scaleHeight);
		if (this.scaleWidth != null) ScaleTypes.WIDTH.getScaleData(player).setTargetScale(scaleWidth);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (this.scaleHeight != null) outNbt.putFloat(KEY_SCALE_HEIGHT, this.scaleHeight);
		if (this.scaleWidth != null) outNbt.putFloat(KEY_SCALE_WIDTH, this.scaleWidth);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.scaleHeight = nbt.contains(KEY_SCALE_HEIGHT) ? nbt.getFloat(KEY_SCALE_HEIGHT) : null;
		this.scaleWidth = nbt.contains(KEY_SCALE_WIDTH) ? nbt.getFloat(KEY_SCALE_WIDTH) : null;
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, PehkuiCompat::new);
	}
}

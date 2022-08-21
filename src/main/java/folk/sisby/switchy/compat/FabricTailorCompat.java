package folk.sisby.switchy.compat;

import folk.sisby.switchy.api.PresetCompatModule;
import folk.sisby.switchy.api.SwitchyModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.fabrictailor.casts.TailoredPlayer;

public class FabricTailorCompat extends PresetCompatModule {
	private static final Identifier ID = new Identifier("switchy", "fabric_tailor");
	private static final boolean isDefault = true;

	public static final String KEY_SKIN_VALUE = "skinValue";
	public static final String KEY_SKIN_SIGNATURE = "skinSignature";

	// Overwritten on save when null
	@Nullable public String skinValue;
	@Nullable public String skinSignature;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		this.skinValue = tailoredPlayer.getSkinValue();
		this.skinSignature = tailoredPlayer.getSkinSignature();
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		if (this.skinValue != null && this.skinSignature != null) {
			tailoredPlayer.setSkin(this.skinValue, this.skinSignature, true);
		}
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (this.skinValue != null) outNbt.putString(KEY_SKIN_VALUE, this.skinValue);
		if (this.skinSignature != null) outNbt.putString(KEY_SKIN_SIGNATURE, this.skinSignature);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.skinValue = nbt.contains(KEY_SKIN_VALUE) ? nbt.getString(KEY_SKIN_VALUE) : null;
		this.skinSignature = nbt.contains(KEY_SKIN_SIGNATURE) ? nbt.getString(KEY_SKIN_SIGNATURE) : null;
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
		SwitchyModuleRegistry.registerModule(ID, FabricTailorCompat::new);
	}
}

package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class FabricTailorCompatData implements SwitchySerializable {
	public static final Identifier ID = new Identifier("switchy", "fabric_tailor");

	public static final String KEY_SKIN_VALUE = "skinValue";
	public static final String KEY_SKIN_SIGNATURE = "skinSignature";

	// Overwritten on save when null
	@Nullable public String skinValue;
	@Nullable public String skinSignature;

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (skinValue != null) outNbt.putString(KEY_SKIN_VALUE, skinValue);
		if (skinSignature != null) outNbt.putString(KEY_SKIN_SIGNATURE, skinSignature);

		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		skinValue = nbt.contains(KEY_SKIN_VALUE) ? nbt.getString(KEY_SKIN_VALUE) : null;
		skinSignature = nbt.contains(KEY_SKIN_SIGNATURE) ? nbt.getString(KEY_SKIN_SIGNATURE) : null;
	}
}

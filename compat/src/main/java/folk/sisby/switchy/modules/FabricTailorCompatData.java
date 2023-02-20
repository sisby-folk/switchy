package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.module.SwitchyModuleData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class FabricTailorCompatData implements SwitchyModuleData {
	public static final Identifier ID = new Identifier("switchy", "fabric_tailor");

	public static final String KEY_SKIN_VALUE = "skinValue";
	public static final String KEY_SKIN_SIGNATURE = "skinSignature";

	// Overwritten on save when null
	@Nullable public String skinValue;
	@Nullable public String skinSignature;

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
}

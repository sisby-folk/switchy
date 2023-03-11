package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The data component of a module that switches player skins from samolego's Fabric Tailor.
 *
 * @author Sisby folk
 * @see SwitchySerializable
 * @see FabricTailorModule
 * @since 1.0.0
 */
public class FabricTailorModuleData implements SwitchySerializable {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "fabric_tailor");

	/**
	 * The NBT key where the base64 encoded json skin value is stored.
	 */
	public static final String KEY_SKIN_VALUE = "skinValue";
	/**
	 * The NBT key where the skin signature is stored.
	 */
	public static final String KEY_SKIN_SIGNATURE = "skinSignature";

	/**
	 * The base64 encoded json skin value.
	 */
	@Nullable public String skinValue;
	/**
	 * The skin signature.
	 */
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

package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sisby folk
 * @see SwitchySerializable
 * @see FabricTailorCompat
 * The data component of a module that switches nicknames from unascribed's Drogtor The Nickinator
 * @since 1.0.0
 */
public class DrogtorCompatData implements SwitchySerializable {
	/**
	 * Identifier for this module
	 */
	public static final Identifier ID = new Identifier("switchy", "drogtor");

	/**
	 * The NBT key where the nickname is stored
	 */
	public static final String KEY_NICKNAME = "nickname";
	/**
	 * The NBT key where the color name is stored
	 */
	public static final String KEY_NAME_COLOR = "nameColor";
	/**
	 * The NBT key where the "bio" (hover text) is stored
	 */
	public static final String KEY_BIO = "bio";

	/**
	 * The raw nickname
	 */
	@Nullable public String nickname;
	/**
	 * The nickname colour
	 */
	@Nullable public Formatting nameColor;
	/**
	 * The "bio" (nickname hover text)
	 */
	@Nullable public String bio;

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (nickname != null) outNbt.putString(KEY_NICKNAME, nickname);
		if (nameColor != null) outNbt.putString(KEY_NAME_COLOR, nameColor.getName());
		if (bio != null) outNbt.putString(KEY_BIO, bio);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		nickname = nbt.contains(KEY_NICKNAME) ? nbt.getString(KEY_NICKNAME) : null;
		nameColor = nbt.contains(KEY_NAME_COLOR) ? Formatting.byName(nbt.getString(KEY_NAME_COLOR)) : null;
		bio = nbt.contains(KEY_BIO) ? nbt.getString(KEY_BIO) : null;
	}
}

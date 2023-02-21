package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.module.SwitchyModuleDisplayable;
import folk.sisby.switchy.api.module.SwitchyModuleData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class DrogtorCompatData implements SwitchyModuleData, SwitchyModuleDisplayable {
	public static final Identifier ID = new Identifier("switchy",  "drogtor");

	public static final String KEY_NICKNAME = "nickname";
	public static final String KEY_NAME_COLOR = "nameColor";
	public static final String KEY_BIO = "bio";

	// Overwritten on save when null
	@Nullable public String nickname;
	@Nullable public Formatting namecolor;
	@Nullable public String bio;

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (this.nickname != null) outNbt.putString(KEY_NICKNAME, this.nickname);
		if (this.namecolor != null) outNbt.putString(KEY_NAME_COLOR, this.namecolor.getName());
		if (this.bio != null) outNbt.putString(KEY_BIO, this.bio);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.nickname = nbt.contains(KEY_NICKNAME) ? nbt.getString(KEY_NICKNAME) : null;
		this.namecolor = nbt.contains(KEY_NAME_COLOR) ? Formatting.byName(nbt.getString(KEY_NAME_COLOR)) : null;
		this.bio = nbt.contains(KEY_BIO) ? nbt.getString(KEY_BIO) : null;
	}

	@Override
	public NbtCompound toDisplayNbt() {
		return toNbt();
	}
}

package folk.sisby.switchy.modules;

import com.unascribed.drogtor.DrogtorPlayer;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DrogtorCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy",  "drogtor");
	private static final boolean isDefault = true;
	private static final ModuleImportable importable = ModuleImportable.ALWAYS_ALLOWED;


	public static final String KEY_NICKNAME = "nickname";
	public static final String KEY_NAME_COLOR = "nameColor";
	public static final String KEY_BIO = "bio";

	// Overwritten on save when null
	@Nullable public String nickname;
	@Nullable public Formatting namecolor;
	@Nullable public String bio;

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		this.nickname = drogtorPlayer.drogtor$getNickname();
		this.namecolor = drogtorPlayer.drogtor$getNameColor();
		this.bio = drogtorPlayer.drogtor$getBio();
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		String oldName = player.getDisplayName().getString();
		drogtorPlayer.drogtor$setNickname(this.nickname);
		String newName = player.getDisplayName().getString();
		if (!Objects.equals(oldName, newName)) Switchy.LOGGER.info("[Switchy] Player Nickname Change: '" + oldName + "' -> '" + newName + "' [" + player.getGameProfile().getName() + "]");
		drogtorPlayer.drogtor$setNameColor(this.namecolor);
		drogtorPlayer.drogtor$setBio(this.bio);
	}

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
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	@Override
	public ModuleImportable getImportable() {
		return importable;
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, DrogtorCompat::new);
	}
}

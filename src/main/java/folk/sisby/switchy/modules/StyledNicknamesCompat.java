package folk.sisby.switchy.modules;

import eu.pb4.stylednicknames.NicknameHolder;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class StyledNicknamesCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy",  "styled_nicknames");
	private static final boolean isDefault = true;
	private static final ModuleImportable importable = ModuleImportable.ALWAYS_ALLOWED;

	public static final String KEY_NICKNAME = "styled_nickname";

	// Overwritten on save when null
	@Nullable public String styled_nickname;

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		NicknameHolder holder = NicknameHolder.of(player);
		this.styled_nickname = holder.sn_get();
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		NicknameHolder holder = NicknameHolder.of(player);
		String oldName = player.getDisplayName().getString();
		holder.sn_set(this.styled_nickname, false);
		String newName = player.getDisplayName().getString();
		if (!Objects.equals(oldName, newName)) Switchy.LOGGER.info("[Switchy] Player Nickname Change: '" + oldName + "' -> '" + newName + "' [" + player.getGameProfile().getName() + "]");
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (this.styled_nickname != null) outNbt.putString(KEY_NICKNAME, this.styled_nickname);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.styled_nickname = nbt.contains(KEY_NICKNAME) ? nbt.getString(KEY_NICKNAME) : null;
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
		PresetModuleRegistry.registerModule(ID, StyledNicknamesCompat::new);
	}
}

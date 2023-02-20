package folk.sisby.switchy.modules;

import eu.pb4.stylednicknames.NicknameHolder;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class StyledNicknamesCompat implements SwitchyModule {
	public static final Identifier ID = new Identifier("switchy",  "styled_nicknames");

	public static final String KEY_NICKNAME = "styled_nickname";

	// Overwritten on save when null
	@Nullable public String styled_nickname;

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		NicknameHolder holder = NicknameHolder.of(player);
		this.styled_nickname = holder.sn_get();
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
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

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		SwitchyModuleRegistry.registerModule(ID, StyledNicknamesCompat::new, true, SwitchyModuleEditable.ALWAYS_ALLOWED);
		StyledNicknamesCompatDisplay.touch();
	}
}

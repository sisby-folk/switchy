package folk.sisby.switchy.modules;

import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.stylednicknames.NicknameHolder;
import folk.sisby.switchy.SwitchyCompat;
import folk.sisby.switchy.api.module.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches nicknames from Patbox's Styled Nicknames.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.7.2
 */
public class StyledNicknamesModule implements SwitchyModule, SwitchyModuleTransferable {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "styled_nicknames");

	/**
	 * The NBT key where the nickname is stored.
	 */
	public static final String KEY_NICKNAME = "styled_nickname";

	/**
	 * The styled nickname, in placeholder API simplified text format.
	 */
	@Nullable public String styled_nickname;

	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyModuleRegistry.registerModule(ID, StyledNicknamesModule::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.ALWAYS_ALLOWED,
				translatable("switchy.modules.switchy.styled_nicknames.description")
			)
				.withDescriptionWhenEnabled(translatable("switchy.modules.switchy.styled_nicknames.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.modules.switchy.styled_nicknames.disabled"))
				.withDeletionWarning(translatable("switchy.modules.switchy.styled_nicknames.warning"))
		);
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		NicknameHolder holder = NicknameHolder.of(player);
		styled_nickname = holder.sn_get();
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		NicknameHolder holder = NicknameHolder.of(player);
		String oldName = player.getDisplayName().getString();
		if (styled_nickname != null) holder.sn_set(styled_nickname, false);
		String newName = player.getDisplayName().getString();
		if (!Objects.equals(oldName, newName))
			SwitchyCompat.LOGGER.info("[Switchy] Player Nickname Change: '" + oldName + "' -> '" + newName + "' [" + player.getGameProfile().getName() + "]");
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (styled_nickname != null) outNbt.putString(KEY_NICKNAME, styled_nickname);
		return outNbt;
	}

	@Override
	public NbtCompound toClientNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (styled_nickname != null) {
			outNbt.putString(KEY_NICKNAME, Text.Serializer.toJsonTree(TextParserUtils.formatText(styled_nickname)).toString());
		}
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		styled_nickname = nbt.contains(KEY_NICKNAME) ? nbt.getString(KEY_NICKNAME) : null;
	}

	/**
	 * @return a text representation of the stored nickname.
	 */
	public Text getText() {
		return styled_nickname != null ? TextParserUtils.formatText(styled_nickname) : null;
	}
}

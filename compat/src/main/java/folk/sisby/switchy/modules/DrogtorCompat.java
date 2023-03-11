package folk.sisby.switchy.modules;

import com.unascribed.drogtor.DrogtorPlayer;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches nicknames from unascribed's Drogtor The Nickinator.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @see DrogtorCompatData
 * @since 1.0.0
 */
public class DrogtorCompat extends DrogtorCompatData implements SwitchyModule, SwitchyModuleTransferable {
	static {
		SwitchyModuleRegistry.registerModule(ID, DrogtorCompat::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.ALWAYS_ALLOWED,
				translatable("switchy.compat.module.drogtor.description")
		)
				.withDescriptionWhenEnabled(translatable("switchy.compat.module.drogtor.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.compat.module.drogtor.disabled"))
				.withDeletionWarning(translatable("switchy.compat.module.drogtor.warning"))
		);
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		nickname = drogtorPlayer.drogtor$getNickname();
		nameColor = drogtorPlayer.drogtor$getNameColor();
		bio = drogtorPlayer.drogtor$getBio();
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		String oldName = player.getDisplayName().getString();
		drogtorPlayer.drogtor$setNickname(nickname);
		String newName = player.getDisplayName().getString();
		if (!Objects.equals(oldName, newName))
			Switchy.LOGGER.info("[Switchy] Player Nickname Change: '" + oldName + "' -> '" + newName + "' [" + player.getGameProfile().getName() + "]");
		drogtorPlayer.drogtor$setNameColor(nameColor);
		drogtorPlayer.drogtor$setBio(bio);
	}

	@Override
	public NbtCompound toClientNbt() {
		return toNbt();
	}
}

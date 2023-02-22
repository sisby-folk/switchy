package folk.sisby.switchy.modules;

import com.unascribed.drogtor.DrogtorPlayer;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleDisplayable;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DrogtorCompat extends DrogtorCompatData implements SwitchyModule, SwitchyModuleDisplayable {
	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		this.nickname = drogtorPlayer.drogtor$getNickname();
		this.namecolor = drogtorPlayer.drogtor$getNameColor();
		this.bio = drogtorPlayer.drogtor$getBio();
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		String oldName = player.getDisplayName().getString();
		drogtorPlayer.drogtor$setNickname(this.nickname);
		String newName = player.getDisplayName().getString();
		if (!Objects.equals(oldName, newName)) Switchy.LOGGER.info("[Switchy] Player Nickname Change: '" + oldName + "' -> '" + newName + "' [" + player.getGameProfile().getName() + "]");
		drogtorPlayer.drogtor$setNameColor(this.namecolor);
		drogtorPlayer.drogtor$setBio(this.bio);
	}

	@Override
	public NbtCompound toDisplayNbt() {
		return toNbt();
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		SwitchyModuleRegistry.registerModule(ID, DrogtorCompat::new, true, SwitchyModuleEditable.ALWAYS_ALLOWED);
	}
}

package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.compat.StyledNicknamesCompat;
import dev.sisby.switchy.duck.SwitchyGameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = Player.class, priority = 1500) // must apply AFTER styled nicknames, as to avoid its cache
public class MixinPlayer {
	@ModifyArg(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"))
	private Component useProfileName(Component text) {
		Player self = (Player) (Object) this;
		if (self instanceof ServerPlayer spe && ((Object) spe.getGameProfile()) instanceof SwitchyGameProfile sgp && sgp.switchy$getSayProfile() != null) {
			MutableComponent name = SwitchyCommands.getNameText(spe, sgp.switchy$getSayProfile());
			return Switchy.STYLED_NICKNAMES ? StyledNicknamesCompat.formatNickname(name) : name;
		}
		return text;
	}
}

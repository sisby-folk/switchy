package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.compat.StyledNicknamesCompat;
import dev.sisby.switchy.duck.SwitchyGameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = PlayerEntity.class, priority = 1500) // must apply AFTER styled nicknames, as to avoid its cache
public class MixinPlayerEntity {
	@ModifyArg(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Team;decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"))
	private Text useProfileName(Text text) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (self instanceof ServerPlayerEntity spe && spe.getGameProfile() instanceof SwitchyGameProfile sgp && sgp.switchy$getSayProfile() != null) {
			MutableText name = SwitchyCommands.getNameText(spe, sgp.switchy$getSayProfile());
			return Switchy.STYLED_NICKNAMES ? StyledNicknamesCompat.formatNickname(name) : name;
		}
		return text;
	}
}

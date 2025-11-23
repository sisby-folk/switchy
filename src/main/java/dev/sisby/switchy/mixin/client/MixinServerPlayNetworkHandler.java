package dev.sisby.switchy.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
	@ModifyExpressionValue(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;isHost()Z"))
	private boolean noSwitchShutdown(boolean original) {
		ServerPlayNetworkHandler self = (ServerPlayNetworkHandler) (Object) this;
		if (Switchy.CONFIG.fastSingleplayerReconnect && self.getPlayer() instanceof SwitchyPlayer sp && sp.switchy$hotSwapData() != null) return false;
		return original;
	}
}

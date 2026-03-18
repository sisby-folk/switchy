package dev.sisby.switchy.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {
	@ModifyExpressionValue(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isSingleplayerOwner()Z"))
	private boolean noSwitchShutdown(boolean original) {
		ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
		if (Switchy.CONFIG.fastSingleplayerReconnect && self.getPlayer() instanceof SwitchyPlayer sp && sp.switchy$hotSwapData() != null) return false;
		return original;
	}
}

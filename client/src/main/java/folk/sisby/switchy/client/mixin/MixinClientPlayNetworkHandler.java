package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records client player commands before they're sent for confirmations.
 *
 * @author Garden System
 * @see SwitchyClientCommands
 * @since 2.0.0
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {
	@Inject(method = "sendCommand", at = @At("HEAD"))
	public void recordCommands(String string, CallbackInfoReturnable<Boolean> cir) {
		SwitchyClientCommands.HISTORY = string;
	}

	@Inject(method = "method_45730", at = @At("HEAD"))
	public void recordChatCommands(String string, CallbackInfo ci) {
		SwitchyClientCommands.HISTORY = string;
	}
}

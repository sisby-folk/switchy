package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records server commands before they're sent into a map of previous commands per player UUID.
 *
 * @author Garden System
 * @see SwitchyClientCommands
 * @since 2.0.0
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

	/**
	 * Intercepts outgoing chat packets and updates the command history using them.
	 *
	 * @param packet a packet being sent to the server.
	 * @param ci     callback info.
	 */
	@Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V")
	public void sendPacket(Packet<?> packet, CallbackInfo ci) {
		if (packet instanceof ChatCommandC2SPacket chatPacket) {
			SwitchyClientCommands.HISTORY = chatPacket.command();
		}
	}
}

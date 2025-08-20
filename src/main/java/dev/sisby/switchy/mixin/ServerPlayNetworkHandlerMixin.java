package dev.sisby.switchy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sisby.switchy.duck.SwitchyPlayHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements SwitchyPlayHandler {
	public boolean switchy$hotswapping = false;

	@Override
	public void switchy$hotSwap() {
		ServerPlayNetworkHandler self = (ServerPlayNetworkHandler) (Object) this;
		switchy$hotswapping = true;
		self.reconfigure();
	}

	@Override
	public boolean switchy$isHotSwap() {
		return switchy$hotswapping;
	}

	@WrapOperation(method = "onAcknowledgeReconfiguration", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;transitionInbound(Lnet/minecraft/network/NetworkState;Lnet/minecraft/network/listener/PacketListener;)V"))
	private <T extends PacketListener> void finishHotSwapOnReconfigure(ClientConnection instance, NetworkState<T> state, T packetListener, Operation<Void> original) {
		original.call(instance, state, packetListener);

		if (packetListener instanceof ServerConfigurationNetworkHandler scnh && switchy$hotswapping) {
			scnh.sendConfigurations();
			switchy$hotswapping = false;
		}
	}
}

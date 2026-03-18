package dev.sisby.switchy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sisby.switchy.duck.SwitchyPlayHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.PacketListener;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl implements SwitchyPlayHandler {
	public boolean switchy$hotswapping = false;

	@Override
	public void switchy$hotSwap() {
		ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
		switchy$hotswapping = true;
		self.switchToConfig();
	}

	@Override
	public boolean switchy$isHotSwap() {
		return switchy$hotswapping;
	}

	@WrapOperation(method = "handleConfigurationAcknowledged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;Lnet/minecraft/network/PacketListener;)V"))
	private <T extends PacketListener> void finishHotSwapOnReconfigure(Connection instance, ProtocolInfo<T> state, T packetListener, Operation<Void> original) {
		original.call(instance, state, packetListener);

		if (packetListener instanceof ServerConfigurationPacketListenerImpl scnh && switchy$hotswapping) {
			scnh.startConfiguration();
			switchy$hotswapping = false;
		}
	}
}

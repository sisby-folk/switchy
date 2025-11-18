package dev.sisby.switchy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sisby.switchy.duck.SwitchyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.integrated.IntegratedServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.Optional;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient implements SwitchyClient {
	@Shadow private @Nullable ClientConnection integratedServerConnection;
	@Shadow private boolean integratedServerRunning;
	@Shadow private @Nullable IntegratedServer server;
	private boolean switchy$hotSwapping = false;
	private IntegratedServer switchy$hotSwap = null;

	@Override
	public void switchy$hotDisconnect() {
		MinecraftClient self = (MinecraftClient) (Object) this;
		switchy$hotSwapping = true;
		self.disconnect();
	}

	@Override
	public void switchy$hotReconnect() {
		MinecraftClient client = (MinecraftClient) (Object) this;
		integratedServerRunning = true;
		Duration duration = Duration.ZERO;
		SocketAddress socketAddress = switchy$hotSwap.getNetworkIo().bindLocal();
		ClientConnection clientConnection = ClientConnection.connectLocal(socketAddress);
		clientConnection.setPacketListener(new ClientLoginNetworkHandler(clientConnection, client, null, null, false, duration, status -> {}));
		clientConnection.send(new HandshakeC2SPacket(socketAddress.toString(), 0, NetworkState.LOGIN));
		clientConnection.send(new LoginHelloC2SPacket(client.getSession().getUsername(), Optional.ofNullable(client.getSession().getUuidOrNull())));
		integratedServerConnection = clientConnection;
		server = switchy$hotSwap;
		switchy$hotSwap = null;
	}

	@WrapOperation(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;isStopping()Z"))
	public boolean skipWaitingForShutdown(IntegratedServer instance, Operation<Boolean> original) {
		if (switchy$hotSwapping) {
			switchy$hotSwap = instance;
			switchy$hotSwapping = false;
			return true;
		}
		return original.call(instance);
	}
}

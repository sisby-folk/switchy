package dev.sisby.switchy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sisby.switchy.duck.SwitchyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.client.server.IntegratedServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.Optional;

@Mixin(Minecraft.class)
public class MixinMinecraft implements SwitchyClient {
	@Shadow private @Nullable Connection pendingConnection;
	@Shadow private boolean isLocalServer;
	@Shadow private @Nullable IntegratedServer singleplayerServer;
	private boolean switchy$hotSwapping = false;
	private IntegratedServer switchy$hotSwap = null;

	@Override
	public void switchy$hotDisconnect() {
		Minecraft self = (Minecraft) (Object) this;
		switchy$hotSwapping = true;
		self.clearLevel();
		switchy$hotSwapping = false;
	}

	@Override
	public boolean switchy$hotReconnect() {
		if (switchy$hotSwap == null) return false;
		Minecraft client = (Minecraft) (Object) this;
		isLocalServer = true;
		Duration duration = Duration.ZERO;
		SocketAddress socketAddress = switchy$hotSwap.getConnection().startMemoryChannel();
		Connection clientConnection = Connection.connectToLocalServer(socketAddress);
		clientConnection.setListener(new ClientHandshakePacketListenerImpl(clientConnection, client, null, null, false, duration, status -> {}));
		clientConnection.send(new ClientIntentionPacket(socketAddress.toString(), 0, ConnectionProtocol.LOGIN));
		clientConnection.send(new ServerboundHelloPacket(client.getUser().getName(), Optional.ofNullable(client.getUser().getProfileId())));
		pendingConnection = clientConnection;
		singleplayerServer = switchy$hotSwap;
		switchy$hotSwap = null;
		return true;
	}

	@WrapOperation(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;isShutdown()Z"))
	public boolean skipWaitingForShutdown(IntegratedServer instance, Operation<Boolean> original) {
		if (switchy$hotSwapping) {
			switchy$hotSwap = instance;
			return true;
		}
		return original.call(instance);
	}
}

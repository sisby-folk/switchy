package dev.sisby.switchy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.duck.SwitchyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
	private String switchy$levelName = null;

	@WrapOperation(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
	private void reconnectOnSwitch(Minecraft client, Screen screen, Operation<Void> original, Component reason) {
		ClientPacketListener self = (ClientPacketListener) (Object) this;
		if (reason.getString().startsWith("[Switchy]") && self.getServerData() != null) {
			ConnectScreen.startConnecting(new TitleScreen(false), Minecraft.getInstance(), ServerAddress.parseString(self.getServerData().ip), self.getServerData(), false);
		} else if (reason.getString().startsWith("[Switchy]") && ((SwitchyClient) client).switchy$hotReconnect()) {
			// pass
		} else if (reason.getString().startsWith("[Switchy]") && switchy$levelName != null) {
			// Reload world manually
			client.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
			client.createWorldOpenFlows().loadLevel(new TitleScreen(), switchy$levelName);
			switchy$levelName = null;
		} else {
			original.call(client, screen);
		}
	}

	@WrapOperation(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;clearLevel()V"))
	private void noShutdownOnSwitchyDisconnect(Minecraft client, Operation<Void> original, Component reason) {
		if (client.hasSingleplayerServer()) {
			switchy$levelName = client.getSingleplayerServer().getWorldData().getLevelName();
		}
		if (Switchy.CONFIG.fastSingleplayerReconnect && reason.getString().startsWith("[Switchy]")) {
			((SwitchyClient) client).switchy$hotDisconnect();
		} else {
			original.call(client);
		}
	}
}

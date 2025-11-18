package dev.sisby.switchy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sisby.switchy.duck.SwitchyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
	@WrapOperation(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
	private void reconnectOnSwitch(MinecraftClient client, Screen screen, Operation<Void> original, Text reason) {
		ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
		if (reason.getString().startsWith("[Switchy]") && self.getServerInfo() != null) {
			ConnectScreen.connect(new TitleScreen(false), MinecraftClient.getInstance(), ServerAddress.parse(self.getServerInfo().address), self.getServerInfo(), false);
		} else if (reason.getString().startsWith("[Switchy]") && ((SwitchyClient) client).switchy$hotReconnect()) {
			// pass
		} else {
			original.call(client, screen);
		}
	}

	@WrapOperation(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;disconnect()V"))
	private void noShutdownOnSwitchyDisconnect(MinecraftClient client, Operation<Void> original, Text reason) {
		if (reason.getString().startsWith("[Switchy]")) {
			((SwitchyClient) client).switchy$hotDisconnect();
		} else {
			original.call(client);
		}
	}
}

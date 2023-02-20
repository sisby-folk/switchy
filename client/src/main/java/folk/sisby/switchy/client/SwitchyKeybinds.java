package folk.sisby.switchy.client;

import com.mojang.blaze3d.platform.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBind;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import static folk.sisby.switchy.SwitchyClientServerNetworking.*;

public class SwitchyKeybinds {
	public static void initializeKeybinds() {
		KeyBind switchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBind(
				"key.switchy.switch",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_SEMICOLON,
				"category.switchy.switchy"
		));
		ClientTickEvents.END.register(client -> {
			while (switchKeyBinding.wasPressed()) {
				if (client.player != null && ClientPlayNetworking.canSend(C2S_REQUEST_DISPLAY_PRESETS)) {
					ClientPlayNetworking.send(C2S_REQUEST_DISPLAY_PRESETS, PacketByteBufs.empty());
				}
			}
		});
	}
}

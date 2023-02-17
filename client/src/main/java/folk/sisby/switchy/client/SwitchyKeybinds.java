package folk.sisby.switchy.client;

import com.mojang.blaze3d.platform.InputUtil;
import folk.sisby.switchy.client.screen.SwitchScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

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
				if (client.player != null) {
					client.player.sendMessage(Text.literal("Switch Was Pressed!"), false);
					MinecraftClient.getInstance().setScreen(new SwitchScreen());
				}
			}
		});
	}
}

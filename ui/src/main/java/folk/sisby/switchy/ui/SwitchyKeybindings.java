package folk.sisby.switchy.ui;

import com.mojang.blaze3d.platform.InputUtil;
import folk.sisby.switchy.SwitchyClientServerNetworking;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.ui.screen.SwitchScreen;
import folk.sisby.switchy.ui.screen.SwitchyDisplayScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBind;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

/**
 * Registration for UI keybindings.
 *
 * @author Sisby folk
 * @since 1.9.0
 */
public class SwitchyKeybindings implements SwitchyClientEvents.Init {
	/**
	 * registers all keybindings for Switchy UI.
	 */
	public static void initializeKeybindings() {
		KeyBind switchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBind(
				"key.switchy.switch",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_SEMICOLON,
				"category.switchy.switchy"
		));
		ClientTickEvents.END.register(client -> {
			while (switchKeyBinding.wasPressed()) {
				if (client.player != null && ClientPlayNetworking.canSend(SwitchyClientServerNetworking.C2S_REQUEST_CLIENT_PRESETS)) {
					client.execute(() -> client.setScreen(new SwitchScreen()));
					SwitchyClientApi.getClientPresets(SwitchyDisplayScreen::updatePresetScreens);
				}
			}
		});
	}

	@Override
	public void onInitialize() {
		initializeKeybindings();
	}
}

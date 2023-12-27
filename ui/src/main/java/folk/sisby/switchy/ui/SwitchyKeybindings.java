package folk.sisby.switchy.ui;

import folk.sisby.switchy.SwitchyClientServerNetworking;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.ui.screen.SwitchScreen;
import folk.sisby.switchy.ui.screen.SwitchyScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registration for UI keybindings.
 *
 * @author Sisby folk
 * @since 1.9.0
 */
public class SwitchyKeybindings {
	/**
	 * registers all keybindings for Switchy UI.
	 */
	public static void initializeKeybindings() {
		KeyBinding switchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.switchy.open",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_SEMICOLON,
				"category.switchy.switchy"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (switchKeyBinding.wasPressed()) {
				if (client.player != null && ClientPlayNetworking.canSend(SwitchyClientServerNetworking.C2S_REQUEST_CLIENT_PRESETS)) {
					client.execute(() -> client.setScreen(new SwitchScreen()));
					SwitchyClientApi.getClientPresets(SwitchyScreen::updatePresetScreens);
				}
			}
		});
	}
}

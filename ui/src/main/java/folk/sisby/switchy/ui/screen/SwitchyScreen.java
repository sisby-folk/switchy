package folk.sisby.switchy.ui.screen;

import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.ui.util.SwitchyFeedbackToast;
import net.minecraft.client.MinecraftClient;

/**
 * An interface for screens that represent or display {@link SwitchyClientPresets}.
 * Able to be updated with new {@link SwitchyClientPresets} when required.
 *
 * @author Garden System
 * @since 2.0.0
 */
public interface SwitchyScreen {
	/**
	 * Updates the screen using with a new Switchy Client presets object.
	 *
	 * @param clientPresets the new client presets to provide the screen.
	 */
	void updatePresets(SwitchyClientPresets clientPresets);

	/**
	 * Runs {@link SwitchyScreen#updatePresets} on the current screen if possible.
	 * Shows a toast based on the provided feedback using {@link SwitchyFeedbackToast}.
	 *
	 * @param feedback the feedback object to show in a toast.
	 * @param clientPresets the new client presets to provide the screen.
	 */
	static void updatePresetScreens(SwitchyFeedback feedback, SwitchyClientPresets clientPresets) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> {
			if (client.currentScreen instanceof SwitchyScreen displayScreen) {
				displayScreen.updatePresets(clientPresets);
				SwitchyFeedbackToast.report(feedback, 4000);
			}
		});
	}
}

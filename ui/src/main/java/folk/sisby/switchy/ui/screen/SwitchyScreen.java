package folk.sisby.switchy.ui.screen;

import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.ui.util.SwitchyFeedbackToast;
import net.minecraft.client.MinecraftClient;

public interface SwitchyScreen {
	void updatePresets(SwitchyClientPresets displayPresets);

	static void updatePresetScreens(SwitchyFeedback feedback, SwitchyClientPresets presets) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> {
			if (client.currentScreen instanceof SwitchyScreen displayScreen) {
				displayScreen.updatePresets(presets);
				SwitchyFeedbackToast.report(feedback, 4000);
			}
		});
	}
}

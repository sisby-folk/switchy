package folk.sisby.switchy.ui;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import org.quiltmc.loader.api.QuiltLoader;

/**
 * Addon Initializer for Switchy UI
 *
 * @author Sisby folk
 * @since 2.4.2
 */
public class SwitchyUI implements SwitchyClientEvents.Init {
	@Override
	public void onInitialize() {
		if (QuiltLoader.isModLoaded("owo")) {
			SwitchyKeybindings.initializeKeybindings();
		}
	}
}

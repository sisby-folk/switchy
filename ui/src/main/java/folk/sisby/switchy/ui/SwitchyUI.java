package folk.sisby.switchy.ui;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Addon Initializer for Switchy UI
 *
 * @author Sisby folk
 * @since 2.4.2
 */
public class SwitchyUI implements SwitchyClientEvents.Init {
	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isModLoaded("owo-ui")) {
			SwitchyKeybindings.initializeKeybindings();
		}
	}
}

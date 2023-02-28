package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.modules.DrogtorCompatDisplay;
import folk.sisby.switchy.client.modules.FabricTailorCompatDisplay;
import folk.sisby.switchy.client.modules.OriginsCompatDisplay;
import folk.sisby.switchy.client.modules.StyledNicknamesCompatDisplay;
import org.quiltmc.loader.api.QuiltLoader;

/**
 * Initializer for the Switchy Compat addon for Switchy Client.
 * Provides display modules for some modules registered by {@link folk.sisby.switchy.SwitchyCompat}.
 *
 * @author Sisby folk
 * @see SwitchyClientEvents
 * @since 2.0.0
 */
public class SwitchyCompatClient implements SwitchyClientEvents.Init {
	@Override
	public void onInitialize() {
		DrogtorCompatDisplay.touch();
		FabricTailorCompatDisplay.touch();
		StyledNicknamesCompatDisplay.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompatDisplay.touch();
	}
}

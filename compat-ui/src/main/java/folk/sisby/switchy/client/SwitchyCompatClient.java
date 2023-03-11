package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.modules.DrogtorClientModule;
import folk.sisby.switchy.client.modules.FabricTailorClientModule;
import folk.sisby.switchy.client.modules.OriginsClientModule;
import folk.sisby.switchy.client.modules.StyledNicknamesClientModule;
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
		DrogtorClientModule.touch();
		FabricTailorClientModule.touch();
		StyledNicknamesClientModule.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsClientModule.touch();
	}
}

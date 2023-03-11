package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.modules.DrogtorCompatUI;
import folk.sisby.switchy.client.modules.FabricTailorCompatUI;
import folk.sisby.switchy.client.modules.OriginsCompatUI;
import folk.sisby.switchy.client.modules.StyledNicknamesCompatUI;
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
		DrogtorCompatUI.touch();
		FabricTailorCompatUI.touch();
		StyledNicknamesCompatUI.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompatUI.touch();
	}
}

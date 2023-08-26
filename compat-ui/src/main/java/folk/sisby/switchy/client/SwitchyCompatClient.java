package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.modules.DrogtorClientModule;
import folk.sisby.switchy.client.modules.FabricTailorClientModule;
import folk.sisby.switchy.client.modules.OriginsClientModule;
import folk.sisby.switchy.client.modules.StyledNicknamesClientModule;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer for the Switchy Compat addon for Switchy Client.
 * Provides display modules for some modules registered by {@link folk.sisby.switchy.SwitchyCompat}.
 *
 * @author Sisby folk
 * @see SwitchyClientEvents
 * @since 2.0.0
 */
@SuppressWarnings("deprecation")
public class SwitchyCompatClient implements SwitchyClientEvents.Init {
	/**
	 * The switchy compat ui namespace.
	 */
	public static final String ID = "switchy_compat_ui";
	/**
	 * The switchy compat ui logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isModLoaded("owo")) {
			DrogtorClientModule.touch();
			FabricTailorClientModule.touch();
			StyledNicknamesClientModule.touch();
			if (FabricLoader.getInstance().isModLoaded("origins")) OriginsClientModule.touch();
			LOGGER.info("[Switchy Compat UI] Initialized!");
		}
	}
}

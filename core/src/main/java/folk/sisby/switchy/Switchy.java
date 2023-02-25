package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.config.QuiltConfig;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sisby folk
 * @since 1.0.0
 * @see SwitchyPlayConnectionListener
 * Initializes core addons by invoking {@link SwitchyEvents.Init}. Responsible for logging initial modules.
 */
public class Switchy implements ModInitializer {
	/**
	 * The switchy namespace
	 */
	public static final String ID = "switchy";

	/**
	 * The switchy logger
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	/**
	 * The config object for switchy, containing the current state of {@code /config/switchy/config.toml}
	 */
	public static final SwitchyConfig CONFIG = QuiltConfig.create(ID, "config", SwitchyConfig.class);

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyEvents.INIT.invoker().onInitialize();
		Switchy.LOGGER.info("[Switchy] Initialized! Registered Modules: " + SwitchyModuleRegistry.getModules());
	}
}

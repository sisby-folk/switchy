package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.config.QuiltConfig;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes core addons by invoking {@link SwitchyEvents.Init}.
 * Responsible for logging initial modules.
 * Works around limitations on init-time {@code events} entrypoints by using post-qsl-init entrypoints.
 *
 * @author Sisby folk
 * @see SwitchyPlayConnectionListener
 * @since 1.0.0
 */
public class Switchy implements ClientModInitializer, DedicatedServerModInitializer {
	/**
	 * The switchy namespace.
	 */
	public static final String ID = "switchy";

	/**
	 * The switchy logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	/**
	 * The config object for switchy, containing the current state of {@code /config/switchy/config.toml}.
	 */
	public static final SwitchyConfig CONFIG = QuiltConfig.create(ID, "config", SwitchyConfig.class);

	private void onInitialize(ModContainer ignored) {
		SwitchyEvents.INIT.invoker().onInitialize();
		Switchy.LOGGER.info("[Switchy] Initialized! Registered Modules: " + SwitchyModuleRegistry.getModules());
	}

	@Override
	public void onInitializeClient(ModContainer mod) {
		onInitialize(mod);
	}

	@Override
	public void onInitializeServer(ModContainer mod) {
		onInitialize(mod);
	}
}

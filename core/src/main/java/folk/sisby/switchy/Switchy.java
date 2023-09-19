package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes core addons by invoking {@link SwitchyEvents.Init}.
 * Responsible for logging initial modules.
 * Works around limitations on init-time entrypoints by using two post-init entrypoints.
 *
 * @author Sisby folk
 * @see SwitchyPlayConnectionListener
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
public class Switchy implements DedicatedServerModInitializer, ClientModInitializer {
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
	@SuppressWarnings("deprecation")
	public static final SwitchyConfig CONFIG = SwitchyConfig.create(FabricLoader.getInstance().getConfigDir(), ID, "config", SwitchyConfig.class);

	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(SwitchyCommands::registerCommands);
		ServerPlayConnectionEvents.JOIN.register(SwitchyPlayConnectionListener::onPlayReady);
		ServerPlayConnectionEvents.DISCONNECT.register(SwitchyPlayConnectionListener::onPlayDisconnect);
		SwitchyEvents.registerEntrypointListeners();
		SwitchyEvents.INIT.invoker().onInitialize();
		Switchy.LOGGER.info("[Switchy] Initialized! Registered Modules: " + SwitchyModuleRegistry.getModules());
	}

	@Override
	public void onInitializeClient() {
		onInitialize();
	}

	@Override
	public void onInitializeServer() {
		onInitialize();
	}
}

package folk.sisby.switchy.client;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod initializer for client-side Switchy Client.
 *
 * @author Sisby folk
 * @since 1.7.0
 */
public class SwitchyClient implements ClientModInitializer {
	/**
	 * the Switchy Client namespace.
	 */
	public static final String ID = "switchy_client";
	/**
	 * the Switchy Client logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(Switchy.ID + "-client");
	/**
	 * the path to export presets to, relative to .minecraft.
	 */
	public static final String EXPORT_PATH = "config/switchy";

	@Override
	public void onInitializeClient() {
		SwitchyClientReceivers.InitializeReceivers();
		ClientCommandRegistrationCallback.EVENT.register(SwitchyClientCommands::register);
		SwitchyClientEvents.registerEntrypointListeners();
		SwitchyClientEvents.INIT.invoker().onInitialize();
		LOGGER.info("[Switchy Client] Initialized! Registered Modules: " + SwitchyClientModuleRegistry.getModules());
	}
}

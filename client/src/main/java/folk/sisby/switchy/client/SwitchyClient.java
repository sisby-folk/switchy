package folk.sisby.switchy.client;

import folk.sisby.switchy.Switchy;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchyClient implements ClientModInitializer {

	public static final String ID = "switchy_client";
	public static final Logger LOGGER = LoggerFactory.getLogger(Switchy.ID + "-client");
	public static final String EXPORT_PATH = "config/switchy";

	@Override
	public void onInitializeClient(ModContainer mod) {
		LOGGER.info("Initializing");
		SwitchyCommandsClient.InitializeCommands();
		SwitchyClientNetworking.InitializeReceivers();
		SwitchyKeybinds.initializeKeybinds();
	}

}

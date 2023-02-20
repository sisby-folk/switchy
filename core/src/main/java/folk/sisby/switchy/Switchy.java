package folk.sisby.switchy;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.config.QuiltConfig;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Switchy implements ModInitializer {

	public static final String ID = "switchy";

	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static final SwitchyConfig CONFIG = QuiltConfig.create(ID, "config", SwitchyConfig.class);

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyCommands.InitializeCommands();
		SwitchyModules.InitializeModules();
		LOGGER.info("Switchy: Initialized!");
	}
}

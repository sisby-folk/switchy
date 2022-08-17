package folk.sisby.switchy;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Switchy implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Switchy");

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyCommands.InitializeCommands();
		LOGGER.info("Switchy Initialized!");
	}

}

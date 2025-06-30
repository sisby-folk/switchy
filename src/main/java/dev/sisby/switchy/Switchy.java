package dev.sisby.switchy;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Switchy implements ModInitializer {
	public static final String ID = "switchy";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Switchy] Twitcha-twitch! A-twitcha-twitch!");
	}
}

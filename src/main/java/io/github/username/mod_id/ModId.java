package io.github.username.mod_id;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModId implements ModInitializer {
	public static final String ID = "mod_id";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize() {
		//LOGGER.info("[Mod ID] pretty pink princess ponies prancing perpendicular");
	}
}

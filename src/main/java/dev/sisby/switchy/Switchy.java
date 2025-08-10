package dev.sisby.switchy;

import dev.sisby.switchy.data.SwitchyComponentTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Switchy implements ModInitializer {
	public static final String ID = "switchy";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static Identifier id(String path) {
		return Identifier.of(ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("[Switchy] Twitcha-twitch! A-twitcha-twitch!");
		SwitchyComponentTypes.init();
		ServerPlayConnectionEvents.JOIN.register(SwitchyCommands::greet);
		CommandRegistrationCallback.EVENT.register(SwitchyCommands::register);
	}
}

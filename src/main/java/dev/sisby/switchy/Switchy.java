package dev.sisby.switchy;

import dev.sisby.switchy.data.ComponentTypeLoader;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
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
		ServerPlayConnectionEvents.JOIN.register(SwitchyCommands::greet);
		CommandRegistrationCallback.EVENT.register(SwitchyCommands::register);
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ComponentTypeLoader());
		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> server.getPlayerManager().getPlayerList().forEach(p -> {
			if (p instanceof SwitchyPlayer sp) sp.switchy$startReload();
		}));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> server.getPlayerManager().getPlayerList().forEach(p -> {
			if (p instanceof SwitchyPlayer sp) sp.switchy$finishReload();
		}));
	}
}

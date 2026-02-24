package dev.sisby.switchy;

import dev.sisby.switchy.data.ComponentTypeLoader;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.exception.NbtException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Switchy implements ModInitializer {
	public static final String ID = "switchy";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	public static final SwitchyConfig CONFIG = SwitchyConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", ID, SwitchyConfig.class);

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
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(id("chat"), ((message, sender, params) -> {
			String string = message.signedBody().content();
			SwitchyPlayerData playerData = SwitchyPlayerData.of(sender);
			for (SwitchyProfile profile : playerData.values()) {
				for (SwitchyComponentTypes.Tag tag : profile.getOrDefault(SwitchyComponentTypes.TAG, new ArrayList<SwitchyComponentTypes.Tag>())) {
					if (string.startsWith(tag.prefix()) && string.endsWith(tag.suffix())) {
						String body = string.substring(tag.prefix().length(), string.length() - tag.suffix().length());
						SwitchyCommands.say(SignedMessage.ofUnsigned(body), sender, profile);
						return false;
					}
				}
			}
			return true;
		}));
	}
}

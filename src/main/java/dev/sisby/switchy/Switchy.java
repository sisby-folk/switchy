package dev.sisby.switchy;

import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyLayerTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Switchy implements ModInitializer {
	public static final String ID = "switchy";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<SwitchyPlayerData> PLAYER_DATA = AttachmentRegistry.create(Switchy.id(SwitchyPlayerData.KEY), b -> b
		.initializer(SwitchyPlayerData::create)
		.persistent(SwitchyPlayerData.CODEC)
		.syncWith(SwitchyPlayerData.PACKET_CODEC, AttachmentSyncPredicate.targetOnly())
		.copyOnDeath()
	);

	public static Identifier id(String path) {
		return Identifier.of(ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("[Switchy] Twitcha-twitch! A-twitcha-twitch!");
		SwitchyLayerTypes.init();
		SwitchyComponentTypes.init();
		CommandRegistrationCallback.EVENT.register(SwitchyCommands::register);
	}
}

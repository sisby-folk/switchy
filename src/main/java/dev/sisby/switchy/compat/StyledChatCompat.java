package dev.sisby.switchy.compat;

import eu.pb4.styledchat.StyledChatUtils;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.ServerCommandSource;

public class StyledChatCompat {
	public static void modifyForSending(SignedMessage message, ServerCommandSource source, RegistryKey<MessageType> type) {
		StyledChatUtils.modifyForSending(message, source, type);
	}
}

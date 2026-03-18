package dev.sisby.switchy.compat;

import eu.pb4.styledchat.StyledChatUtils;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.commands.CommandSourceStack;

public class StyledChatCompat {
	public static void modifyForSending(PlayerChatMessage message, CommandSourceStack source, ResourceKey<ChatType> type) {
		StyledChatUtils.modifyForSending(message, source, type);
	}
}

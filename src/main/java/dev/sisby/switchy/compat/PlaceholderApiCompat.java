package dev.sisby.switchy.compat;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PlaceholderApiCompat {
	public static final Identifier CHAT_HEADS = Identifier.fromNamespaceAndPath("chatheads", "player");

	public static boolean hasHeads() {
	    return Placeholders.getServerPlaceholders().containsKey(CHAT_HEADS);
	}

	public static Component head(MinecraftServer server, String skinHash) {
		return Placeholders.parseServerPlaceholder(CHAT_HEADS, skinHash, ServerPlaceholderContext.of(server)).component();
	}
}

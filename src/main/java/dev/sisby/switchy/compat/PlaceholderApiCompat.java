package dev.sisby.switchy.compat;

//import eu.pb4.placeholders.api.PlaceholderContext;
//import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PlaceholderApiCompat {
	public static final Identifier CHAT_HEADS = Identifier.fromNamespaceAndPath("chatheads", "player");

	public static boolean hasHeads() {
		return false;
		// return Placeholders.getPlaceholders().containsKey(CHAT_HEADS);
	}

	public static Component head(MinecraftServer server, String skinHash) {
		return Component.empty();
		// return Placeholders.parseText(Component.nullToEmpty("%chatheads:player " + skinHash + "%"), PlaceholderContext.of(server));
	}
}

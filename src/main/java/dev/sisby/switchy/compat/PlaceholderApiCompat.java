package dev.sisby.switchy.compat;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PlaceholderApiCompat {
	public static final Identifier CHAT_HEADS = Identifier.of("chatheads", "player");

	public static boolean hasHeads() {
		return Placeholders.getPlaceholders().containsKey(CHAT_HEADS);
	}

	public static Text head(MinecraftServer server, String skinHash) {
		return Placeholders.parseText(Text.of("%chatheads:player " + skinHash + "%"), PlaceholderContext.of(server));
	}
}

package dev.sisby.switchy.compat;

import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.util.FormatUtils;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class StyledChatCompat {
	public static SwitchyComponentType.Builder<String> nameComponent(SwitchyComponentType.Builder<String> b) {
		return b.textProvider((s, v) -> TextParserUtils.formatTextSafe(v));
	}

	public static boolean hasHeads() {
		return Placeholders.getPlaceholders().containsKey(FormatUtils.CHAT_HEADS);
	}

	public static Text head(MinecraftServer server, String skinHash) {
		return Placeholders.parseText(Text.of("%chatheads:player " + skinHash + "%"), PlaceholderContext.of(server));
	}
}

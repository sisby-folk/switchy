package dev.sisby.switchy.compat;

import dev.sisby.switchy.data.SwitchyComponentType;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.parsers.TagParser;
import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class StyledNicknamesCompat {
	public static SwitchyComponentType.Builder<String> nameComponent(SwitchyComponentType.Builder<String> b) {
		return b
			.textProvider((s, v) -> TagParser.SIMPLIFIED_TEXT_FORMAT_SAFE.parseComponent(v, ServerPlaceholderContext.of(s).asParserContext()))
			.playerReader((p, id) -> Optional.ofNullable(PlayerDataApi.getGlobalDataFor(p, Identifier.fromNamespaceAndPath("stylednicknames", "nickname"), StringTag.TYPE)).orElse(StringTag.valueOf(id)).asString().orElse(""))
			.playerMutator((v, p) -> {
				PlayerDataApi.setGlobalDataFor(p, Identifier.fromNamespaceAndPath("stylednicknames", "nickname"), StringTag.valueOf(v));
				PlayerDataApi.setGlobalDataFor(p, Identifier.fromNamespaceAndPath("stylednicknames", "permission"), ByteTag.valueOf(false));
			})
			.initializer((n, p, id) -> id);
	}

	public static Component formatNickname(Component input) {
		return ConfigManager.getConfig().nicknameFormat.toComponent(ParserContext.of(Config.KEY, _ -> input)).copy();
	}
}

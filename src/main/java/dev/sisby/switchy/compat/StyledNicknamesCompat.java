package dev.sisby.switchy.compat;

import dev.sisby.switchy.data.SwitchyComponentType;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;

public class StyledNicknamesCompat {
	public static SwitchyComponentType.Builder<String> nameComponent(SwitchyComponentType.Builder<String> b) {
		return b
			.textProvider((s, v) -> TextParserUtils.formatTextSafe(v))
			.playerReader((p, id) -> Objects.requireNonNullElse(PlayerDataApi.getGlobalDataFor(p, Identifier.fromNamespaceAndPath("stylednicknames", "nickname"), StringTag.TYPE), StringTag.valueOf(id)).getAsString())
			.playerMutator((v, p) -> {
				PlayerDataApi.setGlobalDataFor(p, Identifier.fromNamespaceAndPath("stylednicknames", "nickname"), StringTag.valueOf(v));
				PlayerDataApi.setGlobalDataFor(p, Identifier.fromNamespaceAndPath("stylednicknames", "permission"), ByteTag.valueOf(false));
			})
			.initializer((n, p, id) -> id);
	}

	public static Component formatNickname(Component input) {
		return Placeholders.parseText(ConfigManager.getConfig().nicknameFormat, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, Map.of("nickname", input, "name", input));
	}
}

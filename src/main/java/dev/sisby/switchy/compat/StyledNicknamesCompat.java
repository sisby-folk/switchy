package dev.sisby.switchy.compat;

import dev.sisby.switchy.data.SwitchyComponentType;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;

public class StyledNicknamesCompat {
	public static SwitchyComponentType.Builder<String> nameComponent(SwitchyComponentType.Builder<String> b) {
		return b
			.textProvider((s, v) -> TextParserUtils.formatTextSafe(v))
			.playerReader((p, id) -> Objects.requireNonNullElse(PlayerDataApi.getGlobalDataFor(p, Identifier.of("stylednicknames", "nickname"), NbtString.TYPE), NbtString.of(id)).asString())
			.playerMutator((v, p) -> {
				PlayerDataApi.setGlobalDataFor(p, Identifier.of("stylednicknames", "nickname"), NbtString.of(v));
				PlayerDataApi.setGlobalDataFor(p, Identifier.of("stylednicknames", "permission"), NbtByte.of(false));
			})
			.initializer((n, p, id) -> id);
	}

	public static Text formatNickname(Text input) {
		return Placeholders.parseText(ConfigManager.getConfig().nicknameFormat, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, Map.of("nickname", input, "name", input));
	}
}

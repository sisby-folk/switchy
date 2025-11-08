package dev.sisby.switchy.compat;

import dev.sisby.switchy.data.SwitchyComponentType;
import eu.pb4.playerdata.api.PlayerDataApi;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class StyledNicknamesCompat {
	public static SwitchyComponentType.Builder<String> nicknameComponent(SwitchyComponentType.Builder<String> b) {
		return b
			.playerReader((p, id) -> Objects.requireNonNullElse(PlayerDataApi.getGlobalDataFor(p, Identifier.of("stylednicknames", "nickname"), NbtString.TYPE), NbtString.of(id)).asString())
			.playerMutator((v, p) -> {
				PlayerDataApi.setGlobalDataFor(p, Identifier.of("stylednicknames", "nickname"), NbtString.of(v));
				PlayerDataApi.setGlobalDataFor(p, Identifier.of("stylednicknames", "permission"), NbtByte.of(false));
			})
			.initializer((n, p, id) -> id)
			.textProvider(s -> Text.literal(s.replaceAll("<[^>]*>", "")).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(s)))));
	}
}

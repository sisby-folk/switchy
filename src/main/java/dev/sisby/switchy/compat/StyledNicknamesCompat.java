package dev.sisby.switchy.compat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import eu.pb4.playerdata.api.PlayerDataApi;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StyledNicknamesCompat {
	public static final SwitchyComponentType<String> NICKNAME = SwitchyComponentTypes.register(new Identifier("styled-nicknames", "nickname"), Codec.STRING, b ->  b
		.playerReader((p) -> PlayerDataApi.getGlobalDataFor(p, new Identifier("stylednicknames", "nickname"), NbtString.TYPE).asString())
		.playerMutator((v, p) -> PlayerDataApi.setGlobalDataFor(p, new Identifier("stylednicknames", "nickname"), NbtString.of(v)))
		.textProvider(s -> Text.of(s.replaceAll("\\([^()]*\\)", "")))
		.argumentEditor(e -> CommandManager.argument("nickname", StringArgumentType.greedyString()).executes(c -> e.execute(c, c.getArgument("nickname", String.class)))));

	public static void init() {
	}
}

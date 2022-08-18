package folk.sisby.switchy.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyPlayer;
import net.minecraft.command.argument.SingletonArgumentInfo;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.command.api.ServerArgumentType;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PresetArgumentType implements ArgumentType<String> {
	public static void touch() {

	}
	static {
		ServerArgumentType.register(
				new Identifier(Switchy.MOD_ID, "argument_presets"),
				PresetArgumentType.class,
				SingletonArgumentInfo.contextFree(PresetArgumentType::new),
				arg -> StringArgumentType.word()
		);
	}

	public static PresetArgumentType preset() {
		return new PresetArgumentType();
	}

	@Override
	public String parse(StringReader reader) {
		return reader.readUnquotedString();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		ServerPlayerEntity player;

		try {
			player = ((ServerCommandSource) context.getSource()).getPlayer();
		} catch (CommandSyntaxException e) {
			Switchy.LOGGER.error("Command wasn't called by a player! (this shouldn't happen!)");
			return ArgumentType.super.listSuggestions(context, builder);
		}

		String remaining = builder.getRemainingLowerCase();

		if (((SwitchyPlayer) player).switchy$getPresets() != null) {
			((SwitchyPlayer) player).switchy$getPresets().getPresetNames().stream()
					.filter((s) -> s.toLowerCase(Locale.ROOT).startsWith(remaining))
					.forEach(builder::suggest);
		}

		return builder.buildFuture();
	}

	private PresetArgumentType() { }
}

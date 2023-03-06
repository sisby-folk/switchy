package folk.sisby.switchy.api.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresetData;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

/**
 * An argument type extending Identifiers that suggests presets from player {@link SwitchyPresets}.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class PresetArgumentType implements ArgumentType<String> {
	private final BiPredicate<SwitchyPresets, SwitchyPreset> suggestionPredicate;
	private final StringArgumentType wordArgument;

	private PresetArgumentType(BiPredicate<SwitchyPresets, SwitchyPreset> suggestionPredicate) {
		this.suggestionPredicate = suggestionPredicate;
		wordArgument = StringArgumentType.word();
	}

	/**
	 * Creates an instance of this argument type.
	 *
	 * @param allowCurrent whether to suggest the current preset.
	 * @return an instance.
	 */
	public static PresetArgumentType create(boolean allowCurrent) {
		return new PresetArgumentType(allowCurrent ? (presets, preset) -> true : (presets, preset) -> !presets.getCurrentPresetName().equalsIgnoreCase(preset.getName()));
	}

	/**
	 * Creates an instance of this argument type.
	 *
	 * @param suggestionPredicate a predicate to test whether to suggest the preset.
	 * @return an instance.
	 */
	public static PresetArgumentType create(BiPredicate<SwitchyPresets, SwitchyPreset> suggestionPredicate) {
		return new PresetArgumentType(suggestionPredicate);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		return wordArgument.parse(reader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (context.getSource() instanceof ServerCommandSource source) {
			try {
				ServerPlayerEntity player = source.getPlayer();
				SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
				CommandSource.suggestMatching(presets.getPresets().values().stream().filter(p -> suggestionPredicate.test(presets, p)).map(SwitchyPresetData::getName), builder);
			} catch (CommandSyntaxException ignored) {
			}
		}
		return builder.buildFuture();
	}

	@Override
	public Collection<String> getExamples() {
		return wordArgument.getExamples();
	}
}

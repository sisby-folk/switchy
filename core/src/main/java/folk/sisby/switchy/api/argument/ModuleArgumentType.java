package folk.sisby.switchy.api.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

/**
 * An argument type extending Identifiers that suggests modules from player {@link SwitchyPresets}.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class ModuleArgumentType implements ArgumentType<Identifier> {
	private final BiPredicate<SwitchyPresets, Identifier> suggestionPredicate;
	private final IdentifierArgumentType idArgument;

	private ModuleArgumentType(BiPredicate<SwitchyPresets, Identifier> suggestionPredicate) {
		this.suggestionPredicate = suggestionPredicate;
		idArgument = IdentifierArgumentType.identifier();
	}

	/**
	 * Creates an instance of this argument type.
	 *
	 * @return an instance.
	 */
	public static ModuleArgumentType create() {
		return new ModuleArgumentType((presets, id) -> true);
	}

	/**
	 * Creates an instance of this argument type.
	 *
	 * @param enabled a value to match module enabled status with. Null means any.
	 * @return an instance.
	 */
	public static ModuleArgumentType create(@Nullable Boolean enabled) {
		return new ModuleArgumentType((presets, id) -> enabled == null || presets.isModuleEnabled(id) == enabled);
	}

	/**
	 * Creates an instance of this argument type.
	 *
	 * @param suggestionPredicate a predicate to test whether to suggest the module.
	 * @return an instance.
	 */
	public static ModuleArgumentType create(BiPredicate<SwitchyPresets, Identifier> suggestionPredicate) {
		return new ModuleArgumentType(suggestionPredicate);
	}

	@Override
	public Identifier parse(StringReader reader) throws CommandSyntaxException {
		return idArgument.parse(reader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (context.getSource() instanceof ServerCommandSource source) {
			try {
				ServerPlayerEntity player = source.getPlayer();
				SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
				CommandSource.suggestIdentifiers(presets.getModules().keySet().stream().filter(id -> suggestionPredicate.test(presets, id)), builder);
			} catch (CommandSyntaxException ignored) {
			}
		}
		return builder.buildFuture();
	}

	@Override
	public Collection<String> getExamples() {
		return idArgument.getExamples();
	}
}

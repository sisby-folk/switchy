package folk.sisby.switchy.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IdentifiersFromNbtArgArgumentType implements ArgumentType<List<Identifier>> {
	private final IdentifiersArgumentType identifiersArgumentType;
	private final String nbtArgument;
	private final @Nullable String excludeArgument;
	private final String nbtListKey;


	protected IdentifiersFromNbtArgArgumentType(String nbtArgument, @Nullable String excludeArgument, String nbtListKey) {
		this.identifiersArgumentType = IdentifiersArgumentType.create();
		this.nbtArgument = nbtArgument;
		this.excludeArgument = excludeArgument;
		this.nbtListKey = nbtListKey;
	}

	public static IdentifiersFromNbtArgArgumentType create(String nbtArgument, @Nullable String excludeArgument, String nbtListKey) {
		return new IdentifiersFromNbtArgArgumentType(nbtArgument, excludeArgument, nbtListKey);
	}


	@Override
	public List<Identifier> parse(StringReader reader) throws CommandSyntaxException {
		return identifiersArgumentType.parse(reader);
	}

	@Override
	public Collection<String> getExamples() {
		return List.of("~");
	}

	@Override
	public <V> CompletableFuture<Suggestions> listSuggestions(CommandContext<V> context, SuggestionsBuilder builder) {
		NbtCompound nbt = context.getArgument(nbtArgument, NbtCompound.class);
		List<String> excludeIds = new ArrayList<>();
		if (excludeArgument != null) {
			@SuppressWarnings("unchecked")
			List<Identifier> idArgs = context.getArgument(excludeArgument, List.class);
			excludeIds.addAll(idArgs.stream().map(Identifier::toString).toList());
		}
		List<String> suggestions = nbt.getList(nbtListKey, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).filter(s -> !excludeIds.contains(s)).toList();
		CommandSource.suggestMatching(suggestions, builder);
		try {
			List<String> usedIds = identifiersArgumentType.parse(new StringReader(builder.getRemaining())).stream().map(Identifier::toString).toList();
			String currentValidString = usedIds.stream().filter(suggestions::contains).collect(Collectors.joining(","));
			if (!currentValidString.isBlank()) {
				suggestions.stream().filter(
						s -> !usedIds.contains(s)
				).map(s -> currentValidString + "," + s).forEach(builder::suggest);
			}
		} catch (CommandSyntaxException ignored) {
		}
		return builder.buildFuture();
	}
}

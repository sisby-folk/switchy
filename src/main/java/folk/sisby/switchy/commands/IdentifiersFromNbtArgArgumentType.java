package folk.sisby.switchy.commands;

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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IdentifiersFromNbtArgArgumentType implements ArgumentType<List<Identifier>> {
	private final IdentifiersArgumentType identifiersArgumentType;
	private final String nbtArgument;
	private final String nbtListKey;


	protected IdentifiersFromNbtArgArgumentType(String nbtArgument, String nbtListKey) {
		this.identifiersArgumentType = IdentifiersArgumentType.create();
		this.nbtArgument = nbtArgument;
		this.nbtListKey = nbtListKey;
	}

	public static IdentifiersFromNbtArgArgumentType create(String nbtArgument, String nbtListKey) {
		return new IdentifiersFromNbtArgArgumentType(nbtArgument, nbtListKey);
	}


	@Override
	public List<Identifier> parse(StringReader reader) throws CommandSyntaxException {
		return identifiersArgumentType.parse(reader);
	}

	@Override
	public <V> CompletableFuture<Suggestions> listSuggestions(CommandContext<V> context, SuggestionsBuilder builder) {
		NbtCompound nbt = context.getArgument(nbtArgument, NbtCompound.class);
		if (nbt != null) {
			CommandSource.suggestMatching(nbt.getList(nbtListKey, NbtElement.STRING_TYPE).stream().map(NbtElement::asString), builder);
		}
		return builder.buildFuture();
	}
}

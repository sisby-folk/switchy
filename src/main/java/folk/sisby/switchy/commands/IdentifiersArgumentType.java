package folk.sisby.switchy.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static folk.sisby.switchy.util.Feedback.translatable;

public class IdentifiersArgumentType implements ArgumentType<List<Identifier>> {

	private static boolean isPathCharacterValid(char character) {
		return character == '_'
				|| character == '-'
				|| character >= 'a' && character <= 'z'
				|| character >= '0' && character <= '9'
				|| character == '/'
				|| character == '.';
	}

	private static boolean isNamespaceCharacterValid(char character) {
		return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
	}

	private static final SimpleCommandExceptionType EXPECTED_NAMESPACE = new SimpleCommandExceptionType(translatable("command.exception.identifiers.expected_namespace"));
	private static final SimpleCommandExceptionType EXPECTED_PATH = new SimpleCommandExceptionType(translatable("command.exception.identifiers.expected_path"));

	@Override
	public List<Identifier> parse(StringReader reader) throws CommandSyntaxException {
		List<Identifier> outList = new ArrayList<>();
		if (reader.peek() == '~') {
			return outList;
		}
		do {
			final int start = reader.getCursor();
			if (!reader.canRead() || !isNamespaceCharacterValid(reader.peek())) {
				throw EXPECTED_NAMESPACE.createWithContext(reader);
			}
			while (reader.canRead() && isNamespaceCharacterValid(reader.peek())) {
				reader.skip();
			}
			if (!reader.canRead() || reader.read() != ':' || !reader.canRead() || !isPathCharacterValid(reader.peek())) {
				throw EXPECTED_PATH.createWithContext(reader);
			}
			while (reader.canRead() && isPathCharacterValid(reader.peek())) {
				reader.skip();
			}
			outList.add(new Identifier(reader.getString().substring(start, reader.getCursor())));
		} while (reader.canRead() && reader.read() == ',');
		return outList;
	}

	@Override
	public Collection<String> getExamples() {
		return List.of("~");
	}

	@Override
	public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
		return builder.buildFuture();
	}
}

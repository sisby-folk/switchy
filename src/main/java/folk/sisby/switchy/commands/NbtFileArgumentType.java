package folk.sisby.switchy.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static folk.sisby.switchy.util.Feedback.translatable;

public class NbtFileArgumentType implements ArgumentType<NbtCompound> {
	private final FileArgumentType fileArgumentType;

	protected NbtFileArgumentType(File folder) {
		this.fileArgumentType = FileArgumentType.create(folder, "dat");
	}

	public static NbtFileArgumentType create(File folder) {
		return new NbtFileArgumentType(folder);
	}

	private static final SimpleCommandExceptionType PARSE_FAIL = new SimpleCommandExceptionType(translatable("command.exception.file.invalid_nbt"));

	@Override
	public NbtCompound parse(StringReader reader) throws CommandSyntaxException {
		File file = fileArgumentType.parse(reader);
		try {
			NbtCompound nbt = NbtIo.readCompressed(file);
			nbt.putString("filename", FilenameUtils.getBaseName(file.getName()));
			return nbt;
		} catch (IOException e) {
			throw PARSE_FAIL.createWithContext(reader);
		}
	}


	@Override
	public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
		fileArgumentType.listSuggestions(context, builder);
		return builder.buildFuture();
	}
}

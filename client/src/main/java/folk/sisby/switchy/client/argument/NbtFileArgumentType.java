package folk.sisby.switchy.client.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * An argument type allowing the user to select any .dat file within a folder, which is parsed as compressed NBT.
 *
 * @author Sisby folk
 * @see FileArgumentType
 * @since 1.7.3
 */
public class NbtFileArgumentType implements ArgumentType<NbtCompound> {
	private static final SimpleCommandExceptionType PARSE_FAIL = new SimpleCommandExceptionType(translatable("command.exception.file.invalid_nbt"));
	private final FileArgumentType fileArgumentType;

	NbtFileArgumentType(File folder) {
		this.fileArgumentType = FileArgumentType.create(folder, "dat");
	}

	/**
	 * Creates an instance of this argument type.
	 *
	 * @param folder the folder to allow files to be picked from.
	 * @return an instance.
	 */
	public static NbtFileArgumentType create(File folder) {
		return new NbtFileArgumentType(folder);
	}

	@Override
	public NbtCompound parse(StringReader reader) throws CommandSyntaxException {
		File file = fileArgumentType.parse(reader);
		try {
			NbtCompound nbt = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
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

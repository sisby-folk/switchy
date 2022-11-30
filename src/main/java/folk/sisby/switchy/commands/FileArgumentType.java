package folk.sisby.switchy.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import org.apache.commons.compress.utils.FileNameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static folk.sisby.switchy.util.Feedback.translatable;

public class FileArgumentType implements ArgumentType<File> {
	private final File folder;
	private final String extension;

	private FilenameFilter extensionFilter() {
		return (dir, name) -> FileNameUtils.getExtension(name).toLowerCase().equals(extension);
	}

	protected FileArgumentType(final File folder, final String extension) {
		this.folder = folder;
		this.extension = extension;
	}

	public static FileArgumentType create(File folder, String extension) {
		return new FileArgumentType(folder, extension);
	}

	private static final SimpleCommandExceptionType FILE_NOT_FOUND = new SimpleCommandExceptionType(translatable("command.exception.file.not_found"));

	@Override
	public File parse(StringReader reader) throws CommandSyntaxException {
		File[] exportFiles = folder.listFiles(extensionFilter());
		String filename = reader.readUnquotedString();
		if (exportFiles != null) {
			for (File file : exportFiles) {
				if (FileNameUtils.getBaseName(file.getName()).equals(filename)) {
					return file;
				}
			}
		}
		throw FILE_NOT_FOUND.createWithContext(reader);
	}

	@Override
	public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
		File[] exportFiles = folder.listFiles(extensionFilter());
		if (exportFiles != null) {
			CommandSource.suggestMatching(Arrays.stream(exportFiles).map(File::getName).map(FileNameUtils::getBaseName), builder);
		}
		return builder.buildFuture();
	}
}

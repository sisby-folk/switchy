package folk.sisby.switchy.client.util;

import folk.sisby.switchy.client.SwitchyClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static folk.sisby.switchy.util.Feedback.*;

/**
 * Utilities for interacting with the file system on the client.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class SwitchyFiles {
	/**
	 * Gets a list of files in a folder that match the specified extension.
	 *
	 * @param folder    the containing folder to search.
	 * @param extension the case-insensitive file extension to accept.
	 * @return a list of matching files.
	 */
	public static List<File> filesWithExtension(File folder, String extension) {
		File[] files = folder.listFiles((dir, name) -> FileNameUtils.getExtension(name).equalsIgnoreCase(extension));
		return files == null ? new ArrayList<>() : Arrays.stream(files).toList();
	}

	/**
	 * Writes an NBT Compound to a file.
	 *
	 * @param filename the name of the file in the export folder to write to, without extension.
	 * @param nbt      an arbitrary NBT compound.
	 * @param feedback a consumer for text feedback.
	 * @return a reference to the written file.
	 */
	public static File exportNbtToFile(String filename, @Nullable NbtCompound nbt, Consumer<Text> feedback) {
		if (nbt != null) {
			File exportFile = new File(SwitchyClient.EXPORT_PATH + "/" + filename + ".dat");
			boolean ignored = exportFile.getParentFile().mkdirs();
			try {
				NbtIo.writeCompressed(nbt, exportFile);
				feedback.accept(success("commands.switchy_client.export.success", literal("config/switchy/" + filename + ".dat")));
				return exportFile;
			} catch (IOException e) {
				SwitchyClient.LOGGER.error("IO error when copying default configuration", e);
				feedback.accept(invalid("commands.switchy_client.export.fail"));
				return null;
			}
		}
		return null;
	}
}

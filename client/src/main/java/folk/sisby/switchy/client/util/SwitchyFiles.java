package folk.sisby.switchy.client.util;

import folk.sisby.switchy.client.SwitchyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyFiles {
	public static List<File> filesWithExtension(File folder, String extension) {
		File[] files = folder.listFiles((dir, name) -> FileNameUtils.getExtension(name).equalsIgnoreCase(extension));
		return files == null ? new ArrayList<>() : Arrays.stream(files).toList();
	}

	public static File exportNbtToFile(MinecraftClient client, @Nullable NbtCompound nbt, Consumer<Text> feedback) {
		if (nbt != null) {
			String filename = (client.isInSingleplayer() ? "Singleplayer_" : "Multiplayer_") + new SimpleDateFormat("MMM-dd_HH-mm-ss").format(new java.util.Date());
			File exportFile = new File(SwitchyClient.EXPORT_PATH + "/" + filename + ".dat");
			boolean ignored = exportFile.getParentFile().mkdirs();
			try {
				NbtIo.writeCompressed(nbt, exportFile);
				if (client.player != null) {
					feedback.accept(success("commands.switchy_client.export.success", literal("config/switchy/" + filename + ".dat")));
				}
				return exportFile;
			} catch (IOException e) {
				SwitchyClient.LOGGER.error("IO error when copying default configuration", e);
				if (client.player != null) {
					feedback.accept(invalid( "commands.switchy_client.export.fail"));
				}
				return null;
			}
		}
		return null;
	}
}

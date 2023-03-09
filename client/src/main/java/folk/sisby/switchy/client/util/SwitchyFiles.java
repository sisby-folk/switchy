package folk.sisby.switchy.client.util;

import folk.sisby.switchy.client.SwitchyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static folk.sisby.switchy.client.util.FeedbackClient.tellInvalid;
import static folk.sisby.switchy.client.util.FeedbackClient.tellSuccess;
import static folk.sisby.switchy.util.Feedback.literal;

public class SwitchyFiles {
	public static void exportPresetsToFile(MinecraftClient client, @Nullable NbtCompound presetNbt) {
		if (presetNbt != null) {
			String filename = (client.isInSingleplayer() ? "Singleplayer_" : "Multiplayer_") + new SimpleDateFormat("MMM-dd_HH-mm-ss").format(new java.util.Date());
			File exportFile = new File(SwitchyClient.EXPORT_PATH + "/" + filename + ".dat");
			boolean ignored = exportFile.getParentFile().mkdirs();
			try {
				NbtIo.writeCompressed(presetNbt, exportFile);
				if (client.player != null) {
					tellSuccess(client.player, "commands.switchy_client.export.success", literal("config/switchy/" + filename + ".dat"));
				}
			} catch (IOException e) {
				SwitchyClient.LOGGER.error("IO error when copying default configuration", e);
				if (client.player != null) {
					tellInvalid(client.player, "commands.switchy_client.export.fail");
				}
			}
		}
	}
}

package folk.sisby.switchy;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static folk.sisby.switchy.Switchy.S2C_EXPORT;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger(Switchy.ID + "-client");

	@Override
	public void onInitializeClient(ModContainer mod) {
		LOGGER.info("Initializing");

		ClientPlayNetworking.registerGlobalReceiver(S2C_EXPORT, (client, handler, buf, sender) -> {
			try {
				NbtCompound presetNbt = buf.readNbt();
				if (presetNbt != null) {
					String filename = (client.isInSingleplayer() ? "Singleplayer_" : "Multiplayer_") + new SimpleDateFormat("MMM-dd_HH-mm-ss").format(new java.util.Date());
					File exportFile = new File("config/switchy/" + filename + ".dat");
					exportFile.getParentFile().mkdirs();
					NbtIo.writeCompressed(presetNbt, exportFile);
					if (client.player != null) {
						sendClientMessage(client.player, translatableWithArgs("commands.switchy.export.success", FORMAT_SUCCESS, literal("config/switchy/" + filename + ".dat")));
					}
				}
			} catch (IOException e) {
				LOGGER.error("IO error when copying default configuration", e);
				if (client.player != null) {
					sendClientMessage(client.player, translatableWithArgs("commands.switchy.export.fail", FORMAT_INVALID));
				}
			}
		});
	}
}

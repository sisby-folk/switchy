package folk.sisby.switchy;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static folk.sisby.switchy.Switchy.S2C_EXPORT;

public class SwitchyClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger(Switchy.ID + "-client");

	private static void sendClientMessage(ClientPlayerEntity player, Text text) {
		player.sendMessage(new LiteralText("[Switchy Client] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	@Override
	public void onInitializeClient(ModContainer mod) {
		LOGGER.info("Initializing");

		ClientPlayNetworking.registerGlobalReceiver(S2C_EXPORT, (client, handler, buf, sender) -> {
			try {
				NbtCompound presetNbt = buf.readNbt();
				if (presetNbt != null) {
					new File("config/switchy_exports/").mkdirs();
					String filename = new SimpleDateFormat("MMM-dd-HH-mm-ss-SS").format(new java.util.Date());
					File exportFile = new File("config/switchy_exports/" + filename + ".dat");
					NbtIo.writeCompressed(presetNbt, exportFile);
					if (client.player != null) {
						sendClientMessage(client.player, SwitchyCommands.translatableWithArgs("commands.switchy.export.success.client", SwitchyCommands.FORMAT_SUCCESS, SwitchyCommands.literal("config/switchy_exports/" + filename + ".dat")));
					}
				}
			} catch (IOException e) {
				LOGGER.error("IO error when copying default configuration", e);
				if (client.player != null) {
					sendClientMessage(client.player, SwitchyCommands.translatableWithArgs("commands.switchy.export.fail", SwitchyCommands.FORMAT_INVALID));
				}
			}
		});
	}
}

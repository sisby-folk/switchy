package folk.sisby.switchy.client;

import com.mojang.brigadier.context.CommandContext;
import folk.sisby.switchy.api.SwitchySwitchEvent;
import folk.sisby.switchy.argument.IdentifiersFromNbtArgArgumentType;
import folk.sisby.switchy.argument.NbtFileArgumentType;
import folk.sisby.switchy.client.api.SwitchyEventsClient;
import folk.sisby.switchy.client.util.CommandClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.quiltmc.qsl.command.api.client.ClientCommandManager;
import org.quiltmc.qsl.command.api.client.ClientCommandRegistrationCallback;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static folk.sisby.switchy.SwitchyNetworking.*;
import static folk.sisby.switchy.util.Command.consumeEventPacket;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyCommandsClient {
	private static final Map<UUID, String> history = new HashMap<>();

	public static void InitializeCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, cbc, reg) -> dispatcher.register(
				ClientCommandManager.literal("switchy_client")
						.then(ClientCommandManager.literal("import")
								.then(ClientCommandManager.argument("file", NbtFileArgumentType.create(new File(SwitchyClient.EXPORT_PATH)))
										.executes((c) -> CommandClient.unwrapAndExecute(c, history, SwitchyCommandsClient::importPresets, new Pair<>("file", NbtCompound.class)))
										.then(ClientCommandManager.argument("excludeModules", IdentifiersFromNbtArgArgumentType.create("file", null, "enabled"))
												.executes((c) -> CommandClient.unwrapAndExecute(c, history, SwitchyCommandsClient::importPresets, new Pair<>("file", NbtCompound.class), new Pair<>("excludeModules", List.class)))
												.then(ClientCommandManager.argument("opModules", IdentifiersFromNbtArgArgumentType.create("file", "excludeModules", "enabled"))
														.executes((c) -> CommandClient.unwrapAndExecute(c, history, SwitchyCommandsClient::importPresets, new Pair<>("file", NbtCompound.class), new Pair<>("excludeModules", List.class), new Pair<>("opModules", List.class)))
												)
										)
								)
						)
		));
	}

	public static void InitializeReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(S2C_EXPORT, (client, handler, buf, sender) -> exportPresets(client, buf));
		ClientPlayNetworking.registerGlobalReceiver(S2C_SWITCH, (client, handler, buf, sender) -> consumeEventPacket(buf, SwitchySwitchEvent::fromNbt, SwitchyEventsClient::fireSwitch));
	}

	private static void exportPresets(MinecraftClient client, PacketByteBuf buf) {
		NbtCompound presetNbt = buf.readNbt();
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

	private static int importPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules, List<Identifier> opModules) {
		if (!excludeModules.isEmpty()) {
			NbtList excludeModulesNbt = new NbtList();
			excludeModules.stream().map(Identifier::toString).map(NbtString::of).forEach(excludeModulesNbt::add);
			presetsNbt.put("excludeModules", excludeModulesNbt);
		}
		if (!opModules.isEmpty()) {
			NbtList opModulesNbt = new NbtList();
			opModules.stream().map(Identifier::toString).map(NbtString::of).forEach(opModulesNbt::add);
			presetsNbt.put("opModules", opModulesNbt);
		}
		presetsNbt.putString("command", context.getInput());
		ClientPlayNetworking.send(C2S_IMPORT, PacketByteBufs.create().writeNbt(presetsNbt));
		tellSuccess(player, "commands.switchy_client.import.success");
		return 1;
	}

	private static int importPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules) {
		return importPresets(context, player, presetsNbt, excludeModules, List.of());
	}

	private static int importPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player, NbtCompound presetsNbt) {
		return importPresets(context, player, presetsNbt, List.of(), List.of());
	}
}

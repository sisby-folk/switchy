package folk.sisby.switchy.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Function3;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.command.api.client.ClientCommandManager;
import org.quiltmc.qsl.command.api.client.ClientCommandRegistrationCallback;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static folk.sisby.switchy.Switchy.S2C_EXPORT;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyCommandsClient {
	private static final Map<UUID, String> last_command = new HashMap<>();

	public static void InitializeCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, cbc, reg) -> dispatcher.register(
				ClientCommandManager.literal("switchy_client")
						.then(ClientCommandManager.literal("import")
								.then(ClientCommandManager.argument("file", StringArgumentType.word())
										.suggests(SwitchyCommandsClient::suggestExportFiles)
										.executes((c) -> unwrapAndExecute(c, SwitchyCommandsClient::importPresets, new Pair<>("file", String.class)))
										.then(ClientCommandManager.argument("addModules", StringArgumentType.greedyString())
												.executes((c) -> unwrapAndExecute(c, SwitchyCommandsClient::importPresets, new Pair<>("file", String.class), new Pair<>("addModules", String.class)))
										)
								)
						)
		));
	}

	public static void InitializeReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(S2C_EXPORT, (client, handler, buf, sender) -> {
			try {
				NbtCompound presetNbt = buf.readNbt();
				if (presetNbt != null) {
					String filename = (client.isInSingleplayer() ? "Singleplayer_" : "Multiplayer_") + new SimpleDateFormat("MMM-dd_HH-mm-ss").format(new java.util.Date());
					File exportFile = new File(SwitchyClient.EXPORT_PATH + "/" + filename + ".dat");
					exportFile.getParentFile().mkdirs();
					NbtIo.writeCompressed(presetNbt, exportFile);
					if (client.player != null) {
						tellSuccess(client.player, "commands.switchy_client.export.success", literal("config/switchy/" + filename + ".dat"));
					}
				}
			} catch (IOException e) {
				SwitchyClient.LOGGER.error("IO error when copying default configuration", e);
				if (client.player != null) {
					tellInvalid(client.player, "commands.switchy_client.export.fail");
				}
			}
		});
	}

	private static <V, V2> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Function3<ClientPlayerEntity, V, V2, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		// Get context and execute
		ClientPlayerEntity player = context.getSource().getPlayer();
		int result = executeFunction.apply(
				player,
				(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
				(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null)
		);
		// Record previous command (for confirmations)
		last_command.put(player.getUuid(), context.getInput());
		return result;
	}

	private static <V> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, BiFunction<ClientPlayerEntity, V, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, (player, arg, ignored2) -> executeFunction.apply(player, arg), argument, null);
	}

	private static int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Function<ClientPlayerEntity, Integer> executeFunction) {
		return unwrapAndExecute(context, (player, ignored, ignored2) -> executeFunction.apply(player), null, null);
	}

	private static CompletableFuture<Suggestions> suggestExportFiles(CommandContext<QuiltClientCommandSource> context, SuggestionsBuilder builder) {
		String remaining = builder.getRemainingLowerCase();
		File exportPath = new File(SwitchyClient.EXPORT_PATH);
		File[] exportFiles = exportPath.listFiles((dir, name) -> name.toLowerCase().endsWith(".dat"));
		if (exportFiles != null) {
			Arrays.stream(exportFiles)
					.map(File::getName)
					.filter((s) -> s.toLowerCase().startsWith(remaining))
					.forEach(builder::suggest);
		}
		return builder.buildFuture();
	}

	private static int importPresets(ClientPlayerEntity player, String file, String addModules) {
		String filePath = SwitchyClient.EXPORT_PATH + "/" + file;
		File importFile = new File(filePath);
		if (importFile.exists()) {
			try {
				NbtCompound presetNbt = NbtIo.readCompressed(importFile);
				presetNbt.putString("filename", file);
				NbtList addModulesNbt = new NbtList();
				try {
					addModulesNbt.addAll(Arrays.stream(addModules.split(",")).map((id) -> NbtString.of(new Identifier(id).toString())).toList());
				} catch (InvalidIdentifierException e) {
					tellInvalid(player, "commands.switchy_client.import.fail.parse", literal(addModules));
					return 0;
				}
				presetNbt.put("addModules", addModulesNbt);
				ClientPlayNetworking.send(Switchy.C2S_IMPORT, PacketByteBufs.create().writeNbt(presetNbt));
				tellSuccess(player, "commands.switchy_client.import.success");
				return 1;
			} catch (IOException e) {
				tellInvalid(player, "commands.switchy_client.import.fail.parse", literal(file));
				return 0;
			}
		} else {
			tellInvalid(player, "commands.switchy_client.import.fail.read", literal(file));
			return 0;
		}
	}

	private static int importPresets(ClientPlayerEntity player, String file) {
		return importPresets(player, file, "");
	}
}

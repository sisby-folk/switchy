package folk.sisby.switchy.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.client.api.SwitchyEventsClient;
import folk.sisby.switchy.api.SwitchySwitchEvent;
import folk.sisby.switchy.argument.IdentifiersFromNbtArgArgumentType;
import folk.sisby.switchy.argument.NbtFileArgumentType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static folk.sisby.switchy.Switchy.S2C_EXPORT;
import static folk.sisby.switchy.Switchy.S2C_SWITCH;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyCommandsClient {
	private static final Map<UUID, String> last_command = new HashMap<>();

	public static void InitializeCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, cbc, reg) -> dispatcher.register(
				ClientCommandManager.literal("switchy_client")
						.then(ClientCommandManager.literal("import")
								.then(ClientCommandManager.argument("file", NbtFileArgumentType.create(new File(SwitchyClient.EXPORT_PATH)))
										.executes((c) -> unwrapAndExecute(c, SwitchyCommandsClient::importPresets, new Pair<>("file", NbtCompound.class)))
										.then(ClientCommandManager.argument("excludeModules", IdentifiersFromNbtArgArgumentType.create("file", null, "enabled"))
												.executes((c) -> unwrapAndExecute(c, SwitchyCommandsClient::importPresets, new Pair<>("file", NbtCompound.class), new Pair<>("excludeModules", List.class)))
												.then(ClientCommandManager.argument("opModules", IdentifiersFromNbtArgArgumentType.create("file", "excludeModules", "enabled"))
														.executes((c) -> unwrapAndExecute(c, SwitchyCommandsClient::importPresets, new Pair<>("file", NbtCompound.class), new Pair<>("excludeModules", List.class), new Pair<>("opModules", List.class)))
												)
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

		ClientPlayNetworking.registerGlobalReceiver(S2C_SWITCH, (client, handler, buf, sender) -> {
			NbtCompound eventNbt = buf.readNbt();
			if (eventNbt != null) {
				SwitchySwitchEvent event = SwitchySwitchEvent.fromNbt(eventNbt);
				SwitchyEventsClient.fireSwitch(event);
			}
		});
	}
	private static <V, V2, V3> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context,
												Function4<ClientPlayerEntity, V, V2, V3, Integer> executeFunction,
												@Nullable Pair<String, Class<V>> argument,
												@Nullable Pair<String, Class<V2>> argument2,
												@Nullable Pair<String, Class<V3>> argument3
	) {
		// Get context and execute
		ClientPlayerEntity player = context.getSource().getPlayer();
		int result = executeFunction.apply(
				player,
				(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
				(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null),
				(argument3 != null ? context.getArgument(argument3.getLeft(), argument3.getRight()) : null)
		);
		// Record previous command (for confirmations)
		last_command.put(player.getUuid(), context.getInput());
		return result;
	}

	private static <V, V2> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Function3<ClientPlayerEntity, V, V2, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		return unwrapAndExecute(context, (player, arg, arg2, ignored) -> executeFunction.apply(player, arg, arg2), argument, argument2, null);
	}

	private static <V> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, BiFunction<ClientPlayerEntity, V, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, (player, arg, ignored) -> executeFunction.apply(player, arg), argument, null);
	}

	private static int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Function<ClientPlayerEntity, Integer> executeFunction) {
		return unwrapAndExecute(context, (player, ignored) -> executeFunction.apply(player), null);
	}

	private static int importPresets(ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules, List<Identifier> opModules) {
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
		ClientPlayNetworking.send(Switchy.C2S_IMPORT, PacketByteBufs.create().writeNbt(presetsNbt));
		tellSuccess(player, "commands.switchy_client.import.success");
		return 1;
	}

	private static int importPresets(ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules) {
		return importPresets(player, presetsNbt, excludeModules, List.of());
	}

	private static int importPresets(ClientPlayerEntity player, NbtCompound presetsNbt) {
		return importPresets(player, presetsNbt, List.of(), List.of());
	}
}

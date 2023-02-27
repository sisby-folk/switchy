package folk.sisby.switchy.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import folk.sisby.switchy.client.argument.IdentifiersFromNbtArgArgumentType;
import folk.sisby.switchy.client.argument.NbtFileArgumentType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandBuildContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.command.api.client.ClientCommandManager;
import org.quiltmc.qsl.command.api.client.ClientCommandRegistrationCallback;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.util.List;

import static folk.sisby.switchy.SwitchyClientServerNetworking.C2S_IMPORT;
import static folk.sisby.switchy.SwitchyClientServerNetworking.C2S_REQUEST_PRESETS;
import static folk.sisby.switchy.client.util.CommandClient.executeClient;
import static folk.sisby.switchy.client.util.FeedbackClient.tellSuccess;

/**
 * @author Sisby folk
 * @since 1.7.0
 * Registration and logic for client commands.
 */
public class SwitchyClientCommands implements ClientCommandRegistrationCallback {
	public static String HISTORY = "";

	@Override
	@SuppressWarnings("unchecked")
	public void registerCommands(CommandDispatcher<QuiltClientCommandSource> dispatcher, CommandBuildContext buildContext, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(
				ClientCommandManager.literal("switchy_client")
						.requires(source -> ClientPlayNetworking.canSend(C2S_IMPORT))
						.then(ClientCommandManager.literal("import")
								.then(ClientCommandManager.argument("file", NbtFileArgumentType.create(new File(SwitchyClient.EXPORT_PATH)))
										.executes(c -> executeClient(c, (context, player) -> importPresets(context, player, c.getArgument("file", NbtCompound.class))))
										.then(ClientCommandManager.argument("excludeModules", IdentifiersFromNbtArgArgumentType.create("file", null, "enabled"))
												.executes(c -> executeClient(c, (context, player) -> importPresets(context, player, c.getArgument("file", NbtCompound.class), c.getArgument("excludeModules", List.class))))
												.then(ClientCommandManager.argument("opModules", IdentifiersFromNbtArgArgumentType.create("file", "excludeModules", "enabled"))
														.executes(c -> executeClient(c, (context, player) -> importPresets(context, player, c.getArgument("file", NbtCompound.class), c.getArgument("excludeModules", List.class), c.getArgument("opModules", List.class))))
												)
										)
								)
						)
						.then(ClientCommandManager.literal("export")
								.executes(c -> executeClient(c, SwitchyClientCommands::exportPresets)))
		);
	}

	private static void importPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules, List<Identifier> opModules) {
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
	}

	private static void exportPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player) {
		SwitchyClient.LOGGER.info(HISTORY);
		ClientPlayNetworking.send(C2S_REQUEST_PRESETS, PacketByteBufs.empty());
		tellSuccess(player, "commands.switchy_client.export.sent");
	}

	private static void importPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules) {
		importPresets(context, player, presetsNbt, excludeModules, List.of());
	}

	private static void importPresets(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player, NbtCompound presetsNbt) {
		importPresets(context, player, presetsNbt, List.of(), List.of());
	}
}

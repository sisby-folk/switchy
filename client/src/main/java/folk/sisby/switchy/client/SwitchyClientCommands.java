package folk.sisby.switchy.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.argument.IdentifiersArgumentType;
import folk.sisby.switchy.client.argument.IdentifiersFromNbtArgArgumentType;
import folk.sisby.switchy.client.argument.NbtFileArgumentType;
import folk.sisby.switchy.util.Feedback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandBuildContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.List;

import static folk.sisby.switchy.client.util.CommandClient.executeClient;
import static folk.sisby.switchy.client.util.FeedbackClient.sendClientMessage;

/**
 * Registration and logic for client commands.
 *
 * @author Sisby folk
 * @since 1.7.0
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class SwitchyClientCommands {
	/**
	 * A map of the previously executed command, per player UUID.
	 * Can be used for "repeat-style" command confirmation.
	 * If the command in here matches the one being executed, that's a confirmation.
	 */
	public static String HISTORY = "";

	static {
		SwitchyClientEvents.COMMAND_INIT_IMPORT.register(((importArgument, helpTextRegistry) -> importArgument.then(ClientCommandManager.argument("file", NbtFileArgumentType.create(SwitchyClientApi.getExportFolder()))
				.executes(c -> executeClient(c, (command, player) -> importPresets(command, player, c.getArgument("file", NbtCompound.class), List.of(), List.of())))
				.then(ClientCommandManager.argument("excludeModules", IdentifiersFromNbtArgArgumentType.create("file", null, "enabled"))
						.executes(c -> executeClient(c, (command, player) -> importPresets(command, player, c.getArgument("file", NbtCompound.class), c.getArgument("excludeModules", List.class), List.of())))
						.then(ClientCommandManager.argument("opModules", IdentifiersFromNbtArgArgumentType.create("file", "excludeModules", "enabled"))
								.executes(c -> executeClient(c, (command, player) -> importPresets(command, player, c.getArgument("file", NbtCompound.class), c.getArgument("excludeModules", List.class), c.getArgument("opModules", List.class))))
						)
				)
		)));
		SwitchyClientEvents.COMMAND_INIT.register(((rootArgument, helpTextRegistry) -> rootArgument.then(ClientCommandManager.literal("export")
				.executes(c -> executeClient(c, (command, player) -> exportPresets(player, List.of())))
				.then(ClientCommandManager.argument("excludeModules", IdentifiersArgumentType.create())
						.executes(c -> executeClient(c, (command, player) -> exportPresets(player, c.getArgument("excludeModules", List.class))))
				)
		)));
	}

	private static void importPresets(String command, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules, List<Identifier> includeModules) {
		SwitchyClientApi.importPresets(presetsNbt, excludeModules, includeModules, command, (feedback, clientPresets) -> feedback.messages().forEach(t -> sendClientMessage(player, t)));
		sendClientMessage(player, Feedback.success("commands.switchy_client.import.success"));
	}

	private static void exportPresets(ClientPlayerEntity player, List<Identifier> excludeModules) {
		SwitchyClientApi.exportPresetsToFile(excludeModules, null, (feedback, file) -> feedback.messages().forEach(t -> sendClientMessage(player, t)));
		sendClientMessage(player, Feedback.success("commands.switchy_client.export.sent"));
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
		LiteralArgumentBuilder<FabricClientCommandSource> rootArgument = ClientCommandManager.literal("switchy_client");
		LiteralArgumentBuilder<FabricClientCommandSource> importArgument = ClientCommandManager.literal("import");
		rootArgument.requires(source -> SwitchyClientApi.isSwitchyServer());

		SwitchyClientEvents.COMMAND_INIT_IMPORT.invoker().registerCommands(importArgument, (t) -> {
		});

		rootArgument.then(importArgument);

		SwitchyClientEvents.COMMAND_INIT.invoker().registerCommands(rootArgument, (t) -> {
		});

		dispatcher.register(rootArgument);
	}
}

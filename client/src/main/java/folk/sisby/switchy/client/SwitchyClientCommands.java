package folk.sisby.switchy.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
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

import static folk.sisby.switchy.SwitchyClientServerNetworking.*;
import static folk.sisby.switchy.client.util.CommandClient.executeClient;
import static folk.sisby.switchy.client.util.FeedbackClient.tellSuccess;

/**
 * Registration and logic for client commands.
 *
 * @author Sisby folk
 * @since 1.7.0
 */
public class SwitchyClientCommands implements ClientCommandRegistrationCallback {
	/**
	 * A map of the previously executed command, per player UUID.
	 * Can be used for "repeat-style" command confirmation.
	 * If the command in here matches the one being executed, that's a confirmation.
	 */
	public static String HISTORY = "";

	private static void importPresets(String command, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules, List<Identifier> includeModules) {
		if (!excludeModules.isEmpty()) {
			NbtList excludeModulesNbt = new NbtList();
			excludeModules.stream().map(Identifier::toString).map(NbtString::of).forEach(excludeModulesNbt::add);
			presetsNbt.put(KEY_IMPORT_EXCLUDE, excludeModulesNbt);
		}
		if (!includeModules.isEmpty()) {
			NbtList includeModulesNbt = new NbtList();
			includeModules.stream().map(Identifier::toString).map(NbtString::of).forEach(includeModulesNbt::add);
			presetsNbt.put(KEY_IMPORT_INCLUDE, includeModulesNbt);
		}
		presetsNbt.putString(KEY_IMPORT_COMMAND, command);
		ClientPlayNetworking.send(C2S_IMPORT_CONFIRM, PacketByteBufs.create().writeNbt(presetsNbt));
		tellSuccess(player, "commands.switchy_client.import.success");
	}

	private static void exportPresets(String command, ClientPlayerEntity player) {
		ClientPlayNetworking.send(C2S_REQUEST_PRESETS, PacketByteBufs.empty());
		tellSuccess(player, "commands.switchy_client.export.sent");
	}

	private static void importPresets(String command, ClientPlayerEntity player, NbtCompound presetsNbt, List<Identifier> excludeModules) {
		importPresets(command, player, presetsNbt, excludeModules, List.of());
	}

	private static void importPresets(String command, ClientPlayerEntity player, NbtCompound presetsNbt) {
		importPresets(command, player, presetsNbt, List.of(), List.of());
	}

	@Override

	public void registerCommands(CommandDispatcher<QuiltClientCommandSource> dispatcher, CommandBuildContext buildContext, CommandManager.RegistrationEnvironment environment) {
		LiteralArgumentBuilder<QuiltClientCommandSource> rootArgument = ClientCommandManager.literal("switchy_client");
		LiteralArgumentBuilder<QuiltClientCommandSource> importArgument = ClientCommandManager.literal("import");
		rootArgument.requires(source -> ClientPlayNetworking.canSend(C2S_IMPORT_CONFIRM));

		SwitchyClientEvents.COMMAND_INIT_IMPORT.invoker().registerCommands(importArgument, (t) -> {});

		rootArgument.then(importArgument);

		SwitchyClientEvents.COMMAND_INIT.invoker().registerCommands(rootArgument, (t) -> {});

		dispatcher.register(rootArgument);
	}

	static {
		SwitchyClientEvents.COMMAND_INIT_IMPORT.register(((importArgument, helpTextRegistry) -> {
			importArgument.then(ClientCommandManager.argument("file", NbtFileArgumentType.create(new File(SwitchyClient.EXPORT_PATH)))
					.executes(c -> executeClient(c, (command, player) -> importPresets(command, player, c.getArgument("file", NbtCompound.class))))
					.then(ClientCommandManager.argument("excludeModules", IdentifiersFromNbtArgArgumentType.create("file", null, "enabled"))
							.executes(c -> executeClient(c, (command, player) -> importPresets(command, player, c.getArgument("file", NbtCompound.class), c.getArgument("excludeModules", List.class))))
							.then(ClientCommandManager.argument("opModules", IdentifiersFromNbtArgArgumentType.create("file", "excludeModules", "enabled"))
									.executes(c -> executeClient(c, (command, player) -> importPresets(command, player, c.getArgument("file", NbtCompound.class), c.getArgument("excludeModules", List.class), c.getArgument("opModules", List.class))))
							)
					)
			);
		}));
		SwitchyClientEvents.COMMAND_INIT.register(((rootArgument, helpTextRegistry) -> {
			rootArgument.then(ClientCommandManager.literal("export")
							.executes(c -> executeClient(c, SwitchyClientCommands::exportPresets)));
		}));
	}
}

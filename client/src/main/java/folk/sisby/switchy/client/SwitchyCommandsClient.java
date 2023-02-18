package folk.sisby.switchy.client;

import com.mojang.brigadier.context.CommandContext;
import folk.sisby.switchy.argument.IdentifiersFromNbtArgArgumentType;
import folk.sisby.switchy.argument.NbtFileArgumentType;
import folk.sisby.switchy.client.util.CommandClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.quiltmc.qsl.command.api.client.ClientCommandManager;
import org.quiltmc.qsl.command.api.client.ClientCommandRegistrationCallback;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static folk.sisby.switchy.SwitchyNetworking.C2S_IMPORT;
import static folk.sisby.switchy.util.Feedback.tellSuccess;

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

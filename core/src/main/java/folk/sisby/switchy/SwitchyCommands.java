package folk.sisby.switchy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.api.SwitchyApi;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.util.SwitchyCommand;
import net.minecraft.command.CommandBuildContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static folk.sisby.switchy.api.SwitchyApi.HELP_TEXT;
import static folk.sisby.switchy.util.Feedback.helpText;
import static folk.sisby.switchy.util.SwitchyCommand.execute;

/**
 * Registration and logic for core commands.
 *
 * @author Sisby folk
 * @since 1.0.0
 */
public class SwitchyCommands implements CommandRegistrationCallback {
	/**
	 * A map of the previously executed command, per player UUID.
	 * Can be used for "repeat-style" command confirmation.
	 * If the command in here matches the one being executed, that's a confirmation.
	 */
	public static final Map<UUID, String> HISTORY = new HashMap<>();
	/**
	 * Whether to register the import literal.
	 * Internal.
	 */
	public static boolean IMPORT_ENABLED = false;

	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandBuildContext buildContext, CommandManager.RegistrationEnvironment environment) {
		LiteralArgumentBuilder<ServerCommandSource> switchyImport = CommandManager.literal("import");
		LiteralArgumentBuilder<ServerCommandSource> switchyRoot = CommandManager.literal("switchy");

		SwitchyEvents.COMMAND_INIT_IMPORT.invoker().registerCommands(switchyImport, HELP_TEXT::put);
		if (IMPORT_ENABLED) {
			switchyRoot.then(switchyImport);
		}

		SwitchyEvents.COMMAND_INIT.invoker().registerCommands(switchyRoot, HELP_TEXT::put);
		dispatcher.register(switchyRoot);

		dispatcher.register(
				CommandManager.literal("switch")
						.then(SwitchyCommand.presetArgument(false)
								.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.switchPreset(pl, pr, f, c.getArgument("preset", String.class))))));
	}

	static {
		SwitchyEvents.COMMAND_INIT.register((switchyRoot, helpTextRegistry) -> {
			switchyRoot.then(CommandManager.literal("help").executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.displayHelp(pl, f))));
			switchyRoot.then(CommandManager.literal("list").executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.listPresets(pr, f))));
			switchyRoot.then(CommandManager.literal("new")
					.then(CommandManager.argument("name", StringArgumentType.word())
							.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.newPreset(pr, f, c.getArgument("name", String.class))))));
			switchyRoot.then(CommandManager.literal("set")
					.then(SwitchyCommand.presetArgument(false)
							.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.switchPreset(pl, pr, f, c.getArgument("preset", String.class))))));
			switchyRoot.then(CommandManager.literal("delete")
					.then(SwitchyCommand.presetArgument(false)
							.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.deletePreset(pl, pr, f, c.getArgument("preset", String.class))))));
			switchyRoot.then(CommandManager.literal("rename")
					.then(SwitchyCommand.presetArgument(true)
							.then(CommandManager.argument("name", StringArgumentType.word())
									.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.renamePreset(pr, f, c.getArgument("preset", String.class), c.getArgument("name", String.class)))))));
			switchyRoot.then(CommandManager.literal("module")
					.then(CommandManager.literal("help")
							.then(SwitchyCommand.moduleArgument(null)
									.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.displayModuleHelp(pr, f, c.getArgument("module", Identifier.class))))))
					.then(CommandManager.literal("enable")
							.then(SwitchyCommand.moduleArgument(false)
									.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.enableModule(pl, pr, f, c.getArgument("module", Identifier.class))))))
					.then(CommandManager.literal("disable")
							.then(SwitchyCommand.moduleArgument(true)
									.executes(c -> execute(c, (pl, pr, f) -> SwitchyApi.disableModule(pl, pr, f, c.getArgument("module", Identifier.class)))))));

			List.of(helpText("commands.switchy.help.help", "commands.switchy.help.command"),
					helpText("commands.switchy.list.help", "commands.switchy.list.command"),
					helpText("commands.switchy.new.help", "commands.switchy.new.command", "commands.switchy.help.placeholder.preset"),
					helpText("commands.switchy.set.help", "commands.switchy.set.command", "commands.switchy.help.placeholder.preset"),
					helpText("commands.switch.help", "commands.switch.command", "commands.switchy.help.placeholder.preset"),
					helpText("commands.switchy.delete.help", "commands.switchy.delete.command", "commands.switchy.help.placeholder.preset"),
					helpText("commands.switchy.rename.help", "commands.switchy.rename.command", "commands.switchy.help.placeholder.preset", "commands.switchy.help.placeholder.preset"),
					helpText("commands.switchy.module.help.help", "commands.switchy.module.help.command", "commands.switchy.help.placeholder.module"),
					helpText("commands.switchy.module.enable.help", "commands.switchy.module.enable.command", "commands.switchy.help.placeholder.module"),
					helpText("commands.switchy.module.disable.help", "commands.switchy.module.disable.command", "commands.switchy.help.placeholder.module")
			).forEach(t -> helpTextRegistry.accept(t, (p) -> true));
		});
	}
}

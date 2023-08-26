package folk.sisby.switchy.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.SwitchyCommands;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Events emitted by Switchy during its operation.
 * Any class implementing the below interfaces can use the entrypoint {@code events} to be invoked without registration.
 *
 * @author Ami
 * @since 1.8.2
 */
@SuppressWarnings("deprecation")
public final class SwitchyEvents {
	/**
	 * @see Init
	 */
	public static final Event<Init> INIT = EventFactory.createArrayBacked(Init.class, callbacks -> () -> {
		for (Init callback : callbacks) {
			callback.onInitialize();
		}
	});

	/**
	 * @see Switch
	 */
	public static final Event<Switch> SWITCH = EventFactory.createArrayBacked(Switch.class, callbacks -> (player, event) -> {
		for (Switch callback : callbacks) {
			callback.onSwitch(player, event);
		}
	});


	/**
	 * @see CommandInit
	 */
	public static final Event<CommandInit> COMMAND_INIT = EventFactory.createArrayBacked(CommandInit.class, callbacks -> (switchyArgument, helpTextRegistry) -> {
		for (CommandInit callback : callbacks) {
			callback.registerCommands(switchyArgument, helpTextRegistry);
		}
	});

	/**
	 * @see CommandInitImport
	 */
	public static final Event<CommandInitImport> COMMAND_INIT_IMPORT = EventFactory.createArrayBacked(CommandInitImport.class, callbacks -> (importArgument, helpTextRegistry) -> {
		for (CommandInitImport callback : callbacks) {
			callback.registerCommands(importArgument, helpTextRegistry);
			SwitchyCommands.IMPORT_ENABLED = true;
		}
	});

	/**
	 * Occurs when Switchy loads modules during initialization.
	 * Use this event to register your addon modules.
	 *
	 * @see folk.sisby.switchy.api.module.SwitchyModuleRegistry
	 */
	@FunctionalInterface
	public interface Init {
		/**
		 * Occurs when Switchy loads modules during initialization.
		 */
		void onInitialize();
	}

	/**
	 * Occurs when switchy registers its commands.
	 * Can be used to register commands under {@code /switchy}.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface CommandInit {
		/**
		 * @param switchyArgument  the literal {@code /switchy} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}.
		 */
		void registerCommands(LiteralArgumentBuilder<ServerCommandSource> switchyArgument, BiConsumer<Text, Predicate<ServerPlayerEntity>> helpTextRegistry);
	}

	/**
	 * Occurs when switchy registers its import command.
	 * Can be used to register commands under {@code /switchy import}.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface CommandInitImport {
		/**
		 * @param importArgument   the literal {@code /switchy import} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}.
		 *                         Predicate to determine whether commands should be shown.
		 */
		void registerCommands(LiteralArgumentBuilder<ServerCommandSource> importArgument, BiConsumer<Text, Predicate<ServerPlayerEntity>> helpTextRegistry);
	}

	/**
	 * Occurs when a player joins, switches presets, or disconnects.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface Switch {
		/**
		 * @param player The relevant player.
		 * @param event  The switch event that has occurred.
		 */
		void onSwitch(ServerPlayerEntity player, SwitchySwitchEvent event);
	}

	public static void registerEntrypointListeners() {
		FabricLoader.getInstance().getEntrypoints("switchy", SwitchyEvents.Init.class).forEach(INIT::register);
	}
}

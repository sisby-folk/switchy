package folk.sisby.switchy.client.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Events emitted by Switchy during its operation.
 * Mostly forwarded to the client via relays in {@link folk.sisby.switchy.SwitchyClientServerNetworking}.
 * Any class implementing the below interfaces can use the entrypoint {@code events} to be invoked without registration.
 *
 * @author Ami
 * @since 1.8.2
 */
public class SwitchyClientEvents {
	/**
	 * A cached copy of the most recent switch event for use in case of disconnects.
	 */
	public static @Nullable SwitchySwitchEvent PREVIOUS_SWITCH_EVENT = null;

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
	public static final Event<Switch> SWITCH = EventFactory.createArrayBacked(Switch.class, callbacks -> (event) -> {
		for (Switch callback : callbacks) {
			callback.onSwitch(event);
		}
		PREVIOUS_SWITCH_EVENT = event;
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
		}
	});

	/**
	 * Occurs when Switchy Client initializes.
	 * Use this to register your {@link SwitchyClientModule}s.
	 *
	 * @see folk.sisby.switchy.client.SwitchyClient
	 */
	@FunctionalInterface
	public interface Init {
		/**
		 * Occurs when Switchy Client initializes.
		 */
		void onInitialize();
	}

	/**
	 * Occurs when Switchy Client registers its commands.
	 * Can be used to register commands under {@code /switchy}.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface CommandInit {
		/**
		 * @param rootArgument     the literal {@code /switchy_client} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy_client help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}.
		 */
		void registerCommands(LiteralArgumentBuilder<FabricClientCommandSource> rootArgument, Consumer<Text> helpTextRegistry);
	}

	/**
	 * Occurs when Switchy Client registers its import command.
	 * Can be used to register commands under {@code /switchy_client import}.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface CommandInitImport {
		/**
		 * @param importArgument   the literal {@code /switchy_client import} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy_client help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}.
		 */
		void registerCommands(LiteralArgumentBuilder<FabricClientCommandSource> importArgument, Consumer<Text> helpTextRegistry);
	}

	/**
	 * Occurs when a player joins, switches presets, or disconnects.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface Switch {
		/**
		 * @param event The switch event that has occurred.
		 */
		void onSwitch(SwitchySwitchEvent event);
	}

	public static void registerEntrypointListeners() {
		FabricLoader.getInstance().getEntrypoints("switchy_client", Init.class).forEach(INIT::register);
	}
}

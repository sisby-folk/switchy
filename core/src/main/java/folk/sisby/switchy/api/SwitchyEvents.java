package folk.sisby.switchy.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;

import java.util.function.Consumer;

/**
 * Events emitted by Switchy during its operation.
 * Any class implementing the below interfaces can use the entrypoint {@code events} to be invoked without registration.
 *
 * @author Ami
 * @since 1.8.2
 */
public final class SwitchyEvents {
	/**
	 * @see Init
	 */
	public static final Event<Init> INIT = Event.create(Init.class, callbacks -> () -> {
		for (Init callback : callbacks) {
			callback.onInitialize();
		}
	});

	/**
	 * @see Switch
	 */
	public static final Event<Switch> SWITCH = Event.create(Switch.class, callbacks -> (player, event) -> {
		for (Switch callback : callbacks) {
			callback.onSwitch(player, event);
		}
	});

	/**
	 * @see CommandInit
	 */
	public static final Event<CommandInit> COMMAND_INIT = Event.create(CommandInit.class, callbacks -> (switchyArgument, helpTextRegistry) -> {
		for (CommandInit callback : callbacks) {
			callback.registerCommands(switchyArgument, helpTextRegistry);
		}
	});

	/**
	 * Occurs when Switchy loads modules during initialization.
	 * Use this event to register your addon modules.
	 *
	 * @see folk.sisby.switchy.api.module.SwitchyModuleRegistry
	 */
	@FunctionalInterface
	public interface Init extends EventAwareListener {
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
	public interface CommandInit extends EventAwareListener {
		/**
		 * @param switchyArgument the literal {@code /switchy} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}
		 */
		void registerCommands(LiteralArgumentBuilder<ServerCommandSource> switchyArgument, Consumer<Text> helpTextRegistry);
	}

	/**
	 * Occurs when a player joins, switches presets, or disconnects.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface Switch extends EventAwareListener {
		/**
		 * @param player The relevant player.
		 * @param event  The switch event that has occurred.
		 */
		void onSwitch(ServerPlayerEntity player, SwitchySwitchEvent event);
	}
}

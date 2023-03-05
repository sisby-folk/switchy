package folk.sisby.switchy.client.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;
import org.quiltmc.qsl.base.api.event.client.ClientEventAwareListener;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;

import java.util.function.Consumer;

/**
 * Events emitted by Switchy during its operation.
 * Mostly forwarded to the client via relays in {@link folk.sisby.switchy.SwitchyClientServerNetworking}.
 * Any class implementing the below interfaces can use the entrypoint {@code events} to be invoked without registration.
 *
 * @author Ami
 * @since 1.8.2
 */
@ClientOnly
public class SwitchyClientEvents {
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
	public static final Event<Switch> SWITCH = Event.create(Switch.class, callbacks -> (event) -> {
		for (Switch callback : callbacks) {
			callback.onSwitch(event);
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
	 * @see CommandInitImport
	 */
	public static final Event<CommandInitImport> COMMAND_INIT_IMPORT = Event.create(CommandInitImport.class, callbacks -> (importArgument, helpTextRegistry) -> {
		for (CommandInitImport callback : callbacks) {
			callback.registerCommands(importArgument, helpTextRegistry);
		}
	});

	/**
	 * Occurs when Switchy Client initializes.
	 * Use this to register your {@link SwitchyDisplayModule}s.
	 *
	 * @see folk.sisby.switchy.client.SwitchyClient
	 */
	@FunctionalInterface
	public interface Init extends ClientEventAwareListener {
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
	public interface CommandInit extends EventAwareListener {
		/**
		 * @param rootArgument the literal {@code /switchy_client} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy_client help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}
		 */
		void registerCommands(LiteralArgumentBuilder<QuiltClientCommandSource> rootArgument, Consumer<Text> helpTextRegistry);
	}

	/**
	 * Occurs when Switchy Client registers its import command.
	 * Can be used to register commands under {@code /switchy_client import}.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface CommandInitImport extends EventAwareListener {
		/**
		 * @param importArgument the literal {@code /switchy_client import} argument to add to.
		 * @param helpTextRegistry a registry to add lines to {@code /switchy_client help}.
		 *                         Lines should be generated using {@link folk.sisby.switchy.util.Feedback#helpText(String, String, String...)}
		 */
		void registerCommands(LiteralArgumentBuilder<QuiltClientCommandSource> importArgument, Consumer<Text> helpTextRegistry);
	}

	/**
	 * Occurs when a player joins, switches presets, or disconnects.
	 *
	 * @see SwitchySwitchEvent
	 */
	@FunctionalInterface
	public interface Switch extends ClientEventAwareListener {
		/**
		 * @param event The switch event that has occurred.
		 */
		void onSwitch(SwitchySwitchEvent event);
	}
}

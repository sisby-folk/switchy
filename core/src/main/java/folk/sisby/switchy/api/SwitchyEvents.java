package folk.sisby.switchy.api;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;

/**
 * @author Ami
 * @since 1.8.2
 * Events emitted by Switchy during its operation.
 * Any class implementing the below interfaces can use the entrypoint {@code events} to be invoked without registration.
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
	 * @see folk.sisby.switchy.api.module.SwitchyModuleRegistry
	 * Occurs when Switchy loads modules during initialization.
	 * Use this event to register your addon modules.
	 */
	@FunctionalInterface
	public interface Init extends EventAwareListener {
		/**
		 * Occurs when Switchy loads modules during initialization.
		 */
		void onInitialize();
	}

	/**
	 * @see SwitchySwitchEvent
	 * Occurs when a player joins, switches presets, or disconnects.
	 */
	@FunctionalInterface
	public interface Switch extends EventAwareListener {
		/**
		 * @param player The relevant player.
		 * @param event  The switch event that has occurred
		 */
		void onSwitch(ServerPlayerEntity player, SwitchySwitchEvent event);
	}
}

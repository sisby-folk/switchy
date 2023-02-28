package folk.sisby.switchy.client.api;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.client.ClientEventAwareListener;

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

package folk.sisby.switchy.client.api;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.client.ClientEventAwareListener;

/**
 * @author Ami
 * @since 1.8.2
 * Events emitted by Switchy during its operation.
 * Mostly forwarded to the client via relays in {@link folk.sisby.switchy.SwitchyClientServerNetworking}
 * Any class implementing the below interfaces can use the entrypoint {@code events} to be invoked without registration.
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
	 * @see folk.sisby.switchy.client.SwitchyClient
	 * Occurs when Switchy Client initializes
	 * Use this to register your {@link SwitchyDisplayModule}s
	 */
	@FunctionalInterface
	public interface Init extends ClientEventAwareListener {
		/**
		 * Occurs when Switchy Client initializes.
		 */
		void onInitialize();
	}

	/**
	 * @see SwitchySwitchEvent
	 * Occurs when a player joins, switches presets, or disconnects.
	 */
	@FunctionalInterface
	public interface Switch extends ClientEventAwareListener {
		/**
		 * @param event The switch event that has occurred
		 */
		void onSwitch(SwitchySwitchEvent event);
	}
}

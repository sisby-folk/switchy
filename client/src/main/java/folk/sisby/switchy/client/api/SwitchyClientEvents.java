package folk.sisby.switchy.client.api;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
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
	public static final Event<Init> INIT = Event.create(Init.class, callbacks -> () -> {
		for (Init callback : callbacks) {
			callback.onInitialize();
		}
	});

	public static final Event<Switch> SWITCH = Event.create(Switch.class, callbacks -> (event) -> {
		for (Switch callback : callbacks) {
			callback.onSwitch(event);
		}
	});

	@FunctionalInterface
	public interface Init extends ClientEventAwareListener {
		void onInitialize();
	}

	@FunctionalInterface
	public interface Switch extends ClientEventAwareListener {
		void onSwitch(SwitchySwitchEvent event);
	}
}

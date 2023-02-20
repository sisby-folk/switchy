package folk.sisby.switchy.client.api;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.client.ClientEventAwareListener;

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

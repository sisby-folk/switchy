package folk.sisby.switchy.api;

import folk.sisby.switchy.Switchy;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SwitchyClientEvents {
	private static final Map<Identifier, Consumer<SwitchySwitchEvent>> switchListeners = new HashMap<>();

	@SuppressWarnings("unused")
	public static void registerSwitchListener(Identifier id, Consumer<SwitchySwitchEvent> listener) {
		switchListeners.put(id, listener);
	}

	public static void fireSwitch(SwitchySwitchEvent event) {
		for(Map.Entry<Identifier, Consumer<SwitchySwitchEvent>> listener : switchListeners.entrySet()) {
			try {
				listener.getValue().accept(event);
			}
			catch(Throwable t) {
				Switchy.LOGGER.error("Switchy: Switch listener {} threw an exception", listener.getKey(), t);
			}
		}
	}
}

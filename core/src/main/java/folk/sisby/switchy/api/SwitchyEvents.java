package folk.sisby.switchy.api;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;

public final class SwitchyEvents {
	/**
	 * An event for when switchy loads modules during initialization.
	 * Use this to register your addon modules.
	 */
	public static final Event<Init> INIT = Event.create(Init.class, callbacks -> () -> {
		for (Init callback : callbacks) {
			callback.onInitialize();
		}
	});

	/**
	 * An event for when a switch occurs, when a player joins, or when a player leaves.
	 * When a player joins, `previousPreset` will be null
	 */
	public static final Event<Switch> SWITCH = Event.create(Switch.class, callbacks -> (player, event) -> {
		for (Switch callback : callbacks) {
			callback.onSwitch(player, event);
		}
	});

	@FunctionalInterface
	public interface Init extends EventAwareListener {
		void onInitialize();
	}

	@FunctionalInterface
	public interface Switch extends EventAwareListener {
		void onSwitch(ServerPlayerEntity player, SwitchySwitchEvent event);
	}
}

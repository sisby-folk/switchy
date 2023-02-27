package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;

/**
 * @author Sisby folk
 * @since 2.0.0
 * Switchy addon initializer for server-side Switchy Client
 */
public class SwitchyClientServer implements SwitchyEvents.Init {
	@Override
	public void onInitialize() {
		SwitchyClientServerNetworking.InitializeReceivers();
		SwitchyClientServerNetworking.InitializeRelays();
	}
}

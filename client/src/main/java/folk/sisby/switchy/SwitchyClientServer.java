package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;

/**
 * Switchy addon initializer for server-side Switchy Client.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class SwitchyClientServer implements SwitchyEvents.Init {
	@Override
	public void onInitialize() {
		SwitchyClientServerNetworking.InitializeReceivers();
		SwitchyClientServerNetworking.InitializeRelays();
	}
}

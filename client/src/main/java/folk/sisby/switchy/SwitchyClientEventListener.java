package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;

public class SwitchyClientEventListener implements SwitchyEvents.Init {
	@Override
	public void onInitialize() {
		SwitchyClientServerNetworking.InitializeReceivers();
		SwitchyClientServerNetworking.InitializeRelays();
	}
}

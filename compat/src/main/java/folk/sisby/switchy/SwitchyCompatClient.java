package folk.sisby.switchy;

import folk.sisby.switchy.client.api.SwitchyClientModInitializer;
import folk.sisby.switchy.modules.DrogtorCompatClient;
import folk.sisby.switchy.modules.FabricTailorCompatClient;
import folk.sisby.switchy.modules.OriginsCompatClient;
import folk.sisby.switchy.modules.StyledNicknamesCompatClient;
import org.quiltmc.loader.api.QuiltLoader;

public class SwitchyCompatClient implements SwitchyClientModInitializer {
	@Override
	public void onInitialize() {
		// Basically recreate the functionality of every mod because it might not be on the client (ow)
		// We can definitely do better than this for API. writing two NBT parsers makes no sense.

		// Resolution: We need to take a path that allows compat modules to be moved into their respective mods
		// In this case, this really does mean adding client-side addon functionality to server-sided mods like styled.
		// So yes, you may literally need to install styled nicknames onto your client. Patbox is crying. Everyone is crying.

		// If it's serializable or packetable though, we'd be able to parse The module data into something usable on
		// the client side *before* sending it. So that's good.

		// I'd say we probably want a Serializer that handles field storage, and an extension of that interface
		// Think: DrogtorServerModule extends DrogtorModuleSerializer implements SwitchyModule (<DrogtorModuleSerializer>?)
		// interface SwitchyModule extends SwitchyModuleSerializer
		// interface SwitchyClientModule extends SwitchyModuleSerializer

		DrogtorCompatClient.touch();
		StyledNicknamesCompatClient.touch();
		FabricTailorCompatClient.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompatClient.touch();
	}
}

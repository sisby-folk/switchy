package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.modules.*;
import org.quiltmc.loader.api.QuiltLoader;

public class SwitchyCompat implements SwitchyEvents.Init {
	public static final String ID = "switchy_compat";

	@Override
	public void onInitialize() {
		if (QuiltLoader.isModLoaded("drogtor")) DrogtorCompat.touch();
		if (QuiltLoader.isModLoaded("styled-nicknames")) StyledNicknamesCompat.touch();
		if (QuiltLoader.isModLoaded("fabrictailor")) FabricTailorCompat.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompat.touch();
		if (QuiltLoader.isModLoaded("apoli")) ApoliCompat.touch();
		if (QuiltLoader.isModLoaded("pehkui")) PehkuiCompat.touch();
		if (QuiltLoader.isModLoaded("fabrication")) FabricationArmorCompat.touch();
	}
}

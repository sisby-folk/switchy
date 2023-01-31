package folk.sisby.switchy;// Created 2023-30-01T21:57:33

import folk.sisby.switchy.api.SwitchyModInitializer;
import folk.sisby.switchy.modules.*;
import org.quiltmc.loader.api.QuiltLoader;

/**
 * @author KJP12
 * @since ${version}
 **/
public class SwitchyCompat implements SwitchyModInitializer {

	@Override
	public void initializeSwitchyCompat() {
		if (QuiltLoader.isModLoaded("drogtor")) DrogtorCompat.touch();
		if (QuiltLoader.isModLoaded("styled-nicknames")) StyledNicknamesCompat.touch();
		if (QuiltLoader.isModLoaded("fabrictailor")) FabricTailorCompat.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompat.touch();
		if (QuiltLoader.isModLoaded("apoli")) ApoliCompat.touch();
		if (QuiltLoader.isModLoaded("pehkui")) PehkuiCompat.touch();
		if (QuiltLoader.isModLoaded("fabrication")) FabricationArmorCompat.touch();
	}
}

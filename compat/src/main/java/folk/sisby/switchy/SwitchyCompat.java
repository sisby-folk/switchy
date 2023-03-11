package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.modules.*;
import org.quiltmc.loader.api.QuiltLoader;

/**
 * Initializer for the Switchy Compat addon.
 * Provides modules for switching mod data for various mods.
 *
 * @author Ampflower
 * @see SwitchyEvents
 * @since 1.8.4
 */
public class SwitchyCompat implements SwitchyEvents.Init {
	/**
	 * The switchy compat namespace.
	 */
	public static final String ID = "switchy_compat";

	@Override
	public void onInitialize() {
		if (QuiltLoader.isModLoaded("drogtor")) DrogtorModule.touch();
		if (QuiltLoader.isModLoaded("styled-nicknames")) StyledNicknamesModule.touch();
		if (QuiltLoader.isModLoaded("fabrictailor")) FabricTailorModule.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsModule.touch();
		if (QuiltLoader.isModLoaded("apoli")) ApoliModule.touch();
		if (QuiltLoader.isModLoaded("pehkui")) PehkuiModule.touch();
		if (QuiltLoader.isModLoaded("fabrication")) FabricationArmorModule.touch();
	}
}

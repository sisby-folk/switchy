package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.modules.*;
import org.quiltmc.loader.api.QuiltLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	/**
	 * The switchy compat logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize() {
		if (QuiltLoader.isModLoaded("styled-nicknames")) StyledNicknamesModule.touch();
		if (QuiltLoader.isModLoaded("fabrictailor")) FabricTailorModule.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsModule.touch();
		if (QuiltLoader.isModLoaded("apoli")) ApoliModule.touch();
		if (QuiltLoader.isModLoaded("pehkui")) PehkuiModule.touch();
		if (QuiltLoader.isModLoaded("fabrication")) FabricationArmorModule.touch();
		LOGGER.info("[Switchy Compat] Initialized!");
	}
}

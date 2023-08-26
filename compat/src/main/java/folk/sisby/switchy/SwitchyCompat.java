package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.modules.*;
import net.fabricmc.loader.api.FabricLoader;
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
@SuppressWarnings("deprecation")
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
		if (FabricLoader.getInstance().isModLoaded("drogtor") && !FabricLoader.getInstance().isModLoaded("drogstyle")) DrogtorModule.touch();
		if (FabricLoader.getInstance().isModLoaded("styled-nicknames")) StyledNicknamesModule.touch();
		if (FabricLoader.getInstance().isModLoaded("fabrictailor")) FabricTailorModule.touch();
		if (FabricLoader.getInstance().isModLoaded("origins")) OriginsModule.touch();
		if (FabricLoader.getInstance().isModLoaded("apoli")) ApoliModule.touch();
		if (FabricLoader.getInstance().isModLoaded("pehkui")) PehkuiModule.touch();
		if (FabricLoader.getInstance().isModLoaded("fabrication")) FabricationArmorModule.touch();
		LOGGER.info("[Switchy Compat] Initialized!");
	}
}

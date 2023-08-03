package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import net.minecraft.resource.ResourceType;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer for the Switchy Cardinal addon.
 *
 * @author Ampflower
 * @see SwitchyEvents
 * @since 1.8.4
 */
public class SwitchyCardinal implements SwitchyEvents.Init {
	/**
	 * The Switchy Cardinal namespace.
	 */
	public static final String ID = "switchy_cardinal";
	/**
	 * The switchy cardinal logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize() {
		if (QuiltLoader.isModLoaded("cardinal-components-base") && QuiltLoader.isModLoaded("cardinal-components-entity")) {
			ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(CardinalModuleLoader.INSTANCE);
			SwitchyCardinal.LOGGER.info("[Switchy Cardinal] Initialized!");
		}
	}
}

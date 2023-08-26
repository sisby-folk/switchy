package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer for the Switchy Cardinal addon.
 *
 * @author Ampflower
 * @see SwitchyEvents
 * @since 1.8.4
 */
@SuppressWarnings("deprecation")
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
		if (FabricLoader.getInstance().isModLoaded("cardinal-components-base") && FabricLoader.getInstance().isModLoaded("cardinal-components-entity")) {
			ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(CardinalModuleLoader.INSTANCE);
			SwitchyCardinal.LOGGER.info("[Switchy Cardinal] Initialized!");
		}
	}
}

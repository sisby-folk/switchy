package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer for the Switchy Cardinal addon for Switchy Client.
 * Provides a data-driven API for registering previews for Switchy Cardinal modules
 *
 * @author Sisby folk
 * @see SwitchyClientEvents
 * @since 2.6.0
 */
@SuppressWarnings("deprecation")
public class SwitchyCardinalClient implements SwitchyClientEvents.Init {
	/**
	 * The switchy cardinal ui namespace.
	 */
	public static final String ID = "switchy_cardinal_ui";
	/**
	 * The switchy cardinal ui logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isModLoaded("cardinal-components-base") && FabricLoader.getInstance().isModLoaded("cardinal-components-entity")) {
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(CardinalClientModuleLoader.INSTANCE);
			SwitchyCardinalClient.LOGGER.info("[Switchy Cardinal UI] Initialized!");
		}
	}
}

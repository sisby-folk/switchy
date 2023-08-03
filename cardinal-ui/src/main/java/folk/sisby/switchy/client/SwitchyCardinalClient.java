package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import net.minecraft.resource.ResourceType;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;
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
		if (QuiltLoader.isModLoaded("cardinal-components-base") && QuiltLoader.isModLoaded("cardinal-components-entity")) {
			ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(CardinalClientModuleLoader.INSTANCE);
			SwitchyCardinalClient.LOGGER.info("[Switchy Cardinal UI] Initialized!");
		}
	}
}

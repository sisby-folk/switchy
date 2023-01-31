package folk.sisby.switchy;// Created 2023-30-01T21:59:45

import folk.sisby.switchy.api.SwitchyModInitializer;
import folk.sisby.switchy.modules.cardinal.CardinalModuleLoader;
import net.minecraft.resource.ResourceType;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

/**
 * @author KJP12
 * @since ${version}
 **/
public class SwitchyCardinal implements SwitchyModInitializer {
	@Override
	public void initializeSwitchyCompat() {
		ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(CardinalModuleLoader.INSTANCE);
	}
}

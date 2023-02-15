package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyModInitializer;
import net.minecraft.resource.ResourceType;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

public class SwitchyCardinal implements SwitchyModInitializer {
	@Override
	public void initializeSwitchyCompat() {
		if (QuiltLoader.isModLoaded("cardinal-components-base") && QuiltLoader.isModLoaded("cardinal-components-entity")) {
			ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(CardinalModuleLoader.INSTANCE);
		}
	}
}

package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyModInitializer;
import net.minecraft.resource.ResourceType;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

public class SwitchyCardinal implements SwitchyModInitializer {
	@Override
	public void initializeSwitchyCompat() {
		ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(CardinalModuleLoader.INSTANCE);
	}
}

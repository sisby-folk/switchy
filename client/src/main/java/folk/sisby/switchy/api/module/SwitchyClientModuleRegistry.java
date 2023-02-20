package folk.sisby.switchy.api.module;

import folk.sisby.switchy.SwitchyDisplayModules;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class SwitchyClientModuleRegistry {
	public static void registerModule(Identifier moduleId, Supplier<SwitchyDisplayModule<? extends SwitchyModuleData>> moduleConstructor) throws IllegalStateException {
		SwitchyDisplayModules.registerModule(moduleId, moduleConstructor);
	}
}

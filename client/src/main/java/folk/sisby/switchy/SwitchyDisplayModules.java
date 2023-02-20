package folk.sisby.switchy;

import folk.sisby.switchy.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.api.module.SwitchyModuleData;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SwitchyDisplayModules {
	public static final Map<Identifier, Supplier<SwitchyDisplayModule<? extends SwitchyModuleData>>> MODULE_SUPPLIERS = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<SwitchyDisplayModule<? extends SwitchyModuleData>> moduleConstructor) throws IllegalStateException {
		if (MODULE_SUPPLIERS.containsKey(moduleId)) {
			throw new IllegalArgumentException("Specified moduleId is already registered");
		}
		MODULE_SUPPLIERS.put(moduleId, moduleConstructor);
		Switchy.LOGGER.info("Switchy Client: Registered module " + moduleId);
	}
}

package folk.sisby.switchy.client.api.module;

import folk.sisby.switchy.Switchy;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@ClientOnly
public class SwitchyDisplayModuleRegistry {

	public static final Map<Identifier, Supplier<SwitchyDisplayModule>> MODULE_SUPPLIERS = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<SwitchyDisplayModule> moduleConstructor) throws IllegalStateException {
		if (MODULE_SUPPLIERS.containsKey(moduleId)) {
			throw new IllegalArgumentException("Specified moduleId is already registered");
		}
		MODULE_SUPPLIERS.put(moduleId, moduleConstructor);
		Switchy.LOGGER.info("[Switchy Client] Registered display module " + moduleId);
	}
}

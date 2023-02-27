package folk.sisby.switchy.client.api.module;

import folk.sisby.switchy.Switchy;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Sisby folk
 * @since 2.0.0
 * @see SwitchyDisplayModule
 * Provides access to module registration for client display addons.
 * Effectively Static.
 */
@ClientOnly
public class SwitchyDisplayModuleRegistry {
	private static final Map<Identifier, Supplier<SwitchyDisplayModule>> SUPPLIERS = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<SwitchyDisplayModule> moduleConstructor) throws IllegalStateException {
		if (SUPPLIERS.containsKey(moduleId)) {
			throw new IllegalArgumentException("Specified moduleId is already registered");
		}
		SUPPLIERS.put(moduleId, moduleConstructor);
		Switchy.LOGGER.info("[Switchy Client] Registered display module " + moduleId);
	}

	public static @Nullable SwitchyDisplayModule supplyModule(Identifier id) {
		return SUPPLIERS.containsKey(id) ? SUPPLIERS.get(id).get() : null;
	}
}

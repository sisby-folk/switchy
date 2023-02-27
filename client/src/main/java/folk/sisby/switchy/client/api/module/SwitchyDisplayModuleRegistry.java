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
 * @see SwitchyDisplayModule
 * Provides access to module registration for client display addons.
 * Effectively Static.
 * @since 2.0.0
 */
@ClientOnly
public class SwitchyDisplayModuleRegistry {
	private static final Map<Identifier, Supplier<SwitchyDisplayModule>> SUPPLIERS = new HashMap<>();

	/**
	 * Allows addons to register {@link SwitchyDisplayModule} implementations to be used by Switchy Client.
	 *
	 * @param id                A unique identifier to associate with the module being registered.
	 * @param moduleConstructor Usually {@code ModuleName::new} - this will be called on player join.
	 * @throws IllegalArgumentException when {@code id} is already associated with a registered module.
	 */
	public static void registerModule(Identifier id, Supplier<SwitchyDisplayModule> moduleConstructor) throws IllegalStateException {
		if (SUPPLIERS.containsKey(id)) {
			throw new IllegalArgumentException("Specified id is already registered");
		}
		SUPPLIERS.put(id, moduleConstructor);
		Switchy.LOGGER.info("[Switchy Client] Registered display module " + id);
	}

	/**
	 * @param id a module identifier
	 * @return An instance of the module
	 * @see SwitchyDisplayModule
	 */
	public static @Nullable SwitchyDisplayModule supplyModule(Identifier id) {
		return SUPPLIERS.containsKey(id) ? SUPPLIERS.get(id).get() : null;
	}
}

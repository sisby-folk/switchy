package folk.sisby.switchy.client.api.module;

import folk.sisby.switchy.client.SwitchyClient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provides access to module registration for client addons.
 * Effectively Static.
 *
 * @author Sisby folk
 * @see SwitchyClientModule
 * @since 2.0.0
 */
@ClientOnly
public class SwitchyClientModuleRegistry {
	private static final Map<Identifier, Supplier<SwitchyClientModule>> SUPPLIERS = new HashMap<>();

	/**
	 * Allows addons to register {@link SwitchyClientModule} implementations to be used by Switchy Client.
	 *
	 * @param id                A unique identifier to associate with the module being registered.
	 * @param moduleConstructor Usually {@code ModuleName::new} - this will be called on player join.
	 * @throws IllegalArgumentException when {@code id} is already associated with a registered module.
	 */
	public static void registerModule(Identifier id, Supplier<SwitchyClientModule> moduleConstructor) throws IllegalStateException {
		if (SUPPLIERS.containsKey(id)) {
			throw new IllegalArgumentException("Specified id is already registered");
		}
		SUPPLIERS.put(id, moduleConstructor);
		SwitchyClient.LOGGER.info("[Switchy Client] Registered client module " + id);
	}

	/**
	 * Gets an instance of a module using a registered supplier.
	 *
	 * @param id a module identifier.
	 * @return an instance of the module.
	 * @see SwitchyClientModule
	 */
	public static @Nullable SwitchyClientModule supplyModule(Identifier id) {
		return SUPPLIERS.containsKey(id) ? SUPPLIERS.get(id).get() : null;
	}

	/**
	 * Gets the IDs of all registered modules.
	 *
	 * @return a collection of registered module identifiers.
	 */
	public static Collection<Identifier> getModules() {
		return SUPPLIERS.keySet();
	}

	public static boolean containsModule(Identifier id) {
		return SUPPLIERS.containsKey(id);
	}
}

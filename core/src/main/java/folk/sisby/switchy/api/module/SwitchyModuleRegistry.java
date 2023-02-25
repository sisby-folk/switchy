package folk.sisby.switchy.api.module;

import folk.sisby.switchy.Switchy;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Sisby folk
 * @since 2.0.0
 * @see SwitchyModule
 * Provides access to module registration for addons, and exposes information about the current state of the registry.
 * Effectively Static.
 */
public class SwitchyModuleRegistry {
	private static final List<SwitchyModuleEditable> EDITABLE_CONFIGURABLE = List.of(SwitchyModuleEditable.ALLOWED, SwitchyModuleEditable.OPERATOR);

	private static final Map<Identifier, Supplier<SwitchyModule>> SUPPLIERS = new HashMap<>();
	private static final Map<Identifier, SwitchyModuleInfo> INFO = new HashMap<>();

	/**
	 * Allows addons to register {@link SwitchyModule} implementations to be used by Switchy.
	 * @param id A unique identifier to associate with the module being registered.
	 * @param moduleConstructor Usually {@code ModuleName::new} - this will be called on player join.
	 * @param moduleInfo The static settings for the module. See {@link SwitchyModuleInfo}
	 * @throws IllegalArgumentException when {@code id} is already associated with a registered module.
	 * @throws IllegalStateException when a {@code uniqueId} provided in {@link SwitchyModuleInfo} collides with one already registered
	 */
	public static void registerModule(Identifier id, Supplier<SwitchyModule> moduleConstructor, SwitchyModuleInfo moduleInfo) throws IllegalArgumentException, IllegalStateException {
		if (SUPPLIERS.containsKey(id)) {
			throw new IllegalArgumentException("Specified module id is already registered");
		}
		if (INFO.values().stream().anyMatch(info -> info.uniqueIds().stream().anyMatch(moduleInfo.uniqueIds()::contains))) {
			throw new IllegalStateException("Specified uniqueId is already registered");
		}

		INFO.put(id, moduleInfo);
		SUPPLIERS.put(id, moduleConstructor);

		if (EDITABLE_CONFIGURABLE.contains(moduleInfo.editable())) {
			SwitchyModuleEditable configEditable = Switchy.CONFIG.moduleEditable.get(id.toString());
			if (configEditable == null || !EDITABLE_CONFIGURABLE.contains(configEditable)) { // Reset to default
				Switchy.CONFIG.moduleEditable.put(id.toString(), moduleInfo.editable());
			}
		} else {
			Switchy.CONFIG.moduleEditableReadOnly.put(id.toString(), moduleInfo.editable());
		}
		Switchy.LOGGER.info("[Switchy] Registered module " + id);
	}


	/**
	 * @param id a module identifier
	 * @return The cold-editing permissions for the specified module
	 * @see SwitchyModuleEditable
	 */
	public static SwitchyModuleEditable getEditable(Identifier id) {
		SwitchyModuleEditable baseEditable = INFO.get(id).editable();
		return EDITABLE_CONFIGURABLE.contains(baseEditable) ? Switchy.CONFIG.moduleEditable.get(id.toString()) : baseEditable;
	}

	/**
	 * @return All registered module identifiers
	 */
	public static Collection<Identifier> getModules() {
		return INFO.keySet();
	}

	/**
	 * @param id a module identifier
	 * @return Whether the specified module is registered
	 */
	public static boolean containsModule(Identifier id) {
		return INFO.containsKey(id);
	}

	/**
	 * @param id a module identifier
	 * @return Whether a module should be enabled by default
	 */
	public static boolean isDefault(Identifier id) {
		return INFO.get(id).isDefault();
	}

	/**
	 * @param id a module identifier
	 * @return The warning message that should be displayed when disabling the module
	 */
	public static MutableText getDisableConfirmation(Identifier id) {
		return INFO.get(id).disableConfirmation();
	}

	/**
	 * @param id a module identifier
	 * @return Identifiers for modules that must be applied to the player before the specified one during a switch.
	 */
	public static Collection<Identifier> getApplyDependencies(Identifier id) {
		return INFO.get(id).applyDependencies();
	}

	/**
	 * @param id a module identifier
	 * @return An instance of the module
	 * @see SwitchyModule
	 */
	public static SwitchyModule supplyModule(Identifier id) {
		return SUPPLIERS.get(id).get();
	}

	/**
	 * @return A map representing which modules are enabled by default
	 */
	public static Map<Identifier, Boolean> getModuleDefaults() {
		Map<Identifier, Boolean> outMap = new HashMap<>();
		SUPPLIERS.forEach((id, supplier) -> {
			SwitchyModule module = supplier.get();
			if (module != null) {
				outMap.put(id, isDefault(id));
			}
		});
		return outMap;
	}
}

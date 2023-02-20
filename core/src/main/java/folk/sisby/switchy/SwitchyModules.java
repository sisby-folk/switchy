package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SwitchyModules {
	public static final List<SwitchyModuleEditable> IMPORTABLE_CONFIGURABLE = List.of(SwitchyModuleEditable.ALLOWED, SwitchyModuleEditable.OPERATOR);

	public record ModuleInfo(boolean isDefault, SwitchyModuleEditable importable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) {}

	public static final Map<Identifier, Supplier<SwitchyModule>> MODULE_SUPPLIERS = new HashMap<>();
	public static final Map<Identifier, ModuleInfo> MODULE_INFO = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<SwitchyModule> moduleConstructor, boolean isDefault, SwitchyModuleEditable importable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) throws IllegalArgumentException, IllegalStateException {
		if (MODULE_SUPPLIERS.containsKey(moduleId)) {
			throw new IllegalArgumentException("Specified moduleId is already registered");
		}
		if (MODULE_INFO.values().stream().map(ModuleInfo::uniqueIds).anyMatch(ids -> ids.stream().anyMatch(uniqueIds::contains))) {
			throw new IllegalStateException("Specified uniqueId is already registered");
		}

		MODULE_INFO.put(moduleId, new ModuleInfo(isDefault, importable, applyDependencies, uniqueIds, disableConfirmation));
		MODULE_SUPPLIERS.put(moduleId, moduleConstructor);

		if (IMPORTABLE_CONFIGURABLE.contains(importable)) {
			SwitchyModuleEditable configImportable = Switchy.CONFIG.moduleImportable.get(moduleId.toString());
			if (configImportable == null || !IMPORTABLE_CONFIGURABLE.contains(configImportable)) { // Reset to default
				Switchy.CONFIG.moduleImportable.put(moduleId.toString(), importable);
			}
		} else {
			Switchy.CONFIG.moduleImportableReadOnly.put(moduleId.toString(), importable);
		}
		Switchy.LOGGER.info("Switchy: Registered module " + moduleId);
	}

	public static SwitchyModuleEditable getImportable(Identifier moduleId) {
		SwitchyModuleEditable baseImportable = MODULE_INFO.get(moduleId).importable();
		return IMPORTABLE_CONFIGURABLE.contains(baseImportable) ? Switchy.CONFIG.moduleImportable.get(moduleId.toString()) : baseImportable;
	}


	public static void InitializeModules() {
		SwitchyEvents.INIT.invoker().onInitialize();
		Switchy.LOGGER.info("Switchy: Registered Modules: " + MODULE_SUPPLIERS.keySet());
	}
}

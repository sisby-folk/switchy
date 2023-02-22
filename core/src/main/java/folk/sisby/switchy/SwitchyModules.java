package folk.sisby.switchy;

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
	public static final List<SwitchyModuleEditable> EDITABLE_CONFIGURABLE = List.of(SwitchyModuleEditable.ALLOWED, SwitchyModuleEditable.OPERATOR);

	public record ModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) {}

	public static final Map<Identifier, Supplier<SwitchyModule>> MODULE_SUPPLIERS = new HashMap<>();
	public static final Map<Identifier, ModuleInfo> MODULE_INFO = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<SwitchyModule> moduleConstructor, boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) throws IllegalArgumentException, IllegalStateException {
		if (MODULE_SUPPLIERS.containsKey(moduleId)) {
			throw new IllegalArgumentException("Specified moduleId is already registered");
		}
		if (MODULE_INFO.values().stream().map(ModuleInfo::uniqueIds).anyMatch(ids -> ids.stream().anyMatch(uniqueIds::contains))) {
			throw new IllegalStateException("Specified uniqueId is already registered");
		}

		MODULE_INFO.put(moduleId, new ModuleInfo(isDefault, editable, applyDependencies, uniqueIds, disableConfirmation));
		MODULE_SUPPLIERS.put(moduleId, moduleConstructor);

		if (EDITABLE_CONFIGURABLE.contains(editable)) {
			SwitchyModuleEditable configEditable = Switchy.CONFIG.moduleEditable.get(moduleId.toString());
			if (configEditable == null || !EDITABLE_CONFIGURABLE.contains(configEditable)) { // Reset to default
				Switchy.CONFIG.moduleEditable.put(moduleId.toString(), editable);
			}
		} else {
			Switchy.CONFIG.moduleEditableReadOnly.put(moduleId.toString(), editable);
		}
		Switchy.LOGGER.info("[Switchy] Registered module " + moduleId);
	}

	public static SwitchyModuleEditable getEditable(Identifier moduleId) {
		SwitchyModuleEditable baseEditable = MODULE_INFO.get(moduleId).editable();
		return EDITABLE_CONFIGURABLE.contains(baseEditable) ? Switchy.CONFIG.moduleEditable.get(moduleId.toString()) : baseEditable;
	}
}

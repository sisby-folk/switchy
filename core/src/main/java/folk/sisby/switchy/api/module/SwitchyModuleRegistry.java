package folk.sisby.switchy.api.module;

import folk.sisby.switchy.Switchy;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SwitchyModuleRegistry {
	public static final List<SwitchyModuleEditable> EDITABLE_CONFIGURABLE = List.of(SwitchyModuleEditable.ALLOWED, SwitchyModuleEditable.OPERATOR);

	public static final Map<Identifier, Supplier<SwitchyModule>> SUPPLIERS = new HashMap<>();
	public static final Map<Identifier, SwitchyModuleInfo> INFO = new HashMap<>();

	public static void registerModule(Identifier id, Supplier<SwitchyModule> moduleConstructor, SwitchyModuleInfo moduleInfo) throws IllegalArgumentException, IllegalStateException {
		if (SUPPLIERS.containsKey(id)) {
			throw new IllegalArgumentException("Specified module id is already registered");
		}
		if (INFO.values().stream().map(SwitchyModuleInfo::uniqueIds).anyMatch(ids -> ids.stream().anyMatch(moduleInfo.uniqueIds()::contains))) {
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

	public static SwitchyModuleEditable getEditable(Identifier moduleId) {
		SwitchyModuleEditable baseEditable = INFO.get(moduleId).editable();
		return EDITABLE_CONFIGURABLE.contains(baseEditable) ? Switchy.CONFIG.moduleEditable.get(moduleId.toString()) : baseEditable;
	}
}

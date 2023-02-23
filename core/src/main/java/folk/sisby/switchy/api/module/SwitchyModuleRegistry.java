package folk.sisby.switchy.api.module;

import folk.sisby.switchy.Switchy;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SwitchyModuleRegistry {
	private static final List<SwitchyModuleEditable> EDITABLE_CONFIGURABLE = List.of(SwitchyModuleEditable.ALLOWED, SwitchyModuleEditable.OPERATOR);

	private static final Map<Identifier, Supplier<SwitchyModule>> SUPPLIERS = new HashMap<>();
	private static final Map<Identifier, SwitchyModuleInfo> INFO = new HashMap<>();

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

	public static SwitchyModuleEditable getEditable(Identifier id) {
		SwitchyModuleEditable baseEditable = INFO.get(id).editable();
		return EDITABLE_CONFIGURABLE.contains(baseEditable) ? Switchy.CONFIG.moduleEditable.get(id.toString()) : baseEditable;
	}

	public static Collection<Identifier> getModules() {
		return INFO.keySet();
	}

	public static boolean containsModule(Identifier id) {
		return INFO.containsKey(id);
	}

	public static boolean isDefault(Identifier id) {
		return INFO.get(id).isDefault();
	}

	public static MutableText getDisableConfirmation(Identifier id) {
		return INFO.get(id).disableConfirmation();
	}

	public static Collection<Identifier> getApplyDependencies(Identifier id) {
		return INFO.get(id).applyDependencies();
	}

	public static SwitchyModule supplyModule(Identifier id) {
		return SUPPLIERS.get(id).get();
	}

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

	public static Map<Identifier, SwitchyModule> getDefaultModules() {
		Map<Identifier, SwitchyModule> outMap = new HashMap<>();
		SUPPLIERS.forEach((id, supplier) -> {
			if (isDefault(id)) {
				SwitchyModule module = supplier.get();
				if (module != null) {
					outMap.put(id, module);
				}
			}
		});
		return outMap;
	}
}

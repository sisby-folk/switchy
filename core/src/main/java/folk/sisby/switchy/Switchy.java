package folk.sisby.switchy;

import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.SwitchyModInitializer;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.config.QuiltConfig;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Switchy implements ModInitializer {

	public static final String ID = "switchy";

	public static final Logger LOGGER = LoggerFactory.getLogger(ID);


	public static final SwitchyConfig CONFIG = QuiltConfig.create(ID, "config", SwitchyConfig.class);
	public static final List<ModuleImportable> IMPORTABLE_CONFIGURABLE = List.of(ModuleImportable.ALLOWED, ModuleImportable.OPERATOR);

	public record ModuleInfo(boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) {}

	public static final Map<Identifier, Supplier<? extends PresetModule>> MODULE_SUPPLIERS = new HashMap<>();
	public static final Map<Identifier, ModuleInfo> MODULE_INFO = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<? extends PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) throws IllegalArgumentException, IllegalStateException {
		if (MODULE_SUPPLIERS.containsKey(moduleId)) {
			throw new IllegalArgumentException("Specified moduleId is already registered");
		}
		if (MODULE_INFO.values().stream().map(Switchy.ModuleInfo::uniqueIds).anyMatch(ids -> ids.stream().anyMatch(uniqueIds::contains))) {
			throw new IllegalStateException("Specified uniqueId is already registered");
		}

		MODULE_INFO.put(moduleId, new ModuleInfo(isDefault, importable, applyDependencies, uniqueIds, disableConfirmation));
		MODULE_SUPPLIERS.put(moduleId, moduleConstructor);

		if (IMPORTABLE_CONFIGURABLE.contains(importable)) {
			ModuleImportable configImportable = CONFIG.moduleImportable.get(moduleId.toString());
			if (configImportable == null || !IMPORTABLE_CONFIGURABLE.contains(configImportable)) { // Reset to default
				CONFIG.moduleImportable.put(moduleId.toString(), importable);
			}
		} else {
			CONFIG.moduleImportableReadOnly.put(moduleId.toString(), importable);
		}
		LOGGER.info("Switchy: Registered module " + moduleId);
	}

	public static ModuleImportable getImportable(Identifier moduleId) {
		ModuleImportable baseImportable = Switchy.MODULE_INFO.get(moduleId).importable();
		return IMPORTABLE_CONFIGURABLE.contains(baseImportable) ? Switchy.CONFIG.moduleImportable.get(moduleId.toString()) : baseImportable;
	}

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyCommands.InitializeCommands();
		SwitchyEvents.InitializeEvents();
		SwitchyNetworking.InitializeReceivers();

		for(SwitchyModInitializer init : QuiltLoader.getEntrypoints(ID, SwitchyModInitializer.class)) {
			init.initializeSwitchyCompat();
		}

		LOGGER.info("Switchy: Initialized! Already Registered Modules: " + MODULE_SUPPLIERS.keySet());
	}
}

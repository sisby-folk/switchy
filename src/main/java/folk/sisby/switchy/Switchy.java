package folk.sisby.switchy;

import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.commands.SwitchyCommands;
import folk.sisby.switchy.modules.*;
import folk.sisby.switchy.modules.modfest.ModfestCardinalsCompat;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.config.QuiltConfig;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Switchy implements ModInitializer {

	public static final String ID = "switchy";

	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	public static final Identifier S2C_EXPORT = new Identifier(Switchy.ID, "s2c_export");
	public static final Identifier C2S_IMPORT = new Identifier(Switchy.ID, "c2s_import");

	public static final SwitchyConfig CONFIG = QuiltConfig.create(ID, "config", SwitchyConfig.class);
	public static final List<ModuleImportable> IMPORTABLE_CONFIGURABLE = List.of(ModuleImportable.NEVER, ModuleImportable.ALWAYS_ALLOWED);

	public static final Map<Identifier, Supplier<? extends PresetModule>> COMPAT_REGISTRY = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<? extends PresetModule> moduleConstructor) {
		if (!COMPAT_REGISTRY.containsKey(moduleId)) {
			COMPAT_REGISTRY.put(moduleId, moduleConstructor);
			ModuleImportable importable = moduleConstructor.get().getImportable();
			ModuleImportable configImportable = CONFIG.moduleImportable.get(moduleId.toString());

			if (!IMPORTABLE_CONFIGURABLE.contains(importable)) { // Load default config values
				if (configImportable == null || !IMPORTABLE_CONFIGURABLE.contains(configImportable)) {
					CONFIG.moduleImportable.put(moduleId.toString(), moduleConstructor.get().getImportable());
				}
			} else {
				CONFIG.moduleImportableReadOnly.put(moduleId.toString(), moduleConstructor.get().getImportable());
			}
			LOGGER.info("Switchy: Registered module " + moduleId);
		} else {
			LOGGER.error("Switchy: Couldn't register module " + moduleId + "as it was already loaded");
		}
	}

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyCommands.InitializeCommands();
		SwitchyCommands.InitializeReceivers();

		if (QuiltLoader.isModLoaded("drogtor")) DrogtorCompat.touch();
		if (QuiltLoader.isModLoaded("fabrictailor")) FabricTailorCompat.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompat.touch();
		if (QuiltLoader.isModLoaded("apoli")) ApoliCompat.touch();
		if (QuiltLoader.isModLoaded("pehkui")) PehkuiCompat.touch();
		if (QuiltLoader.isModLoaded("fabrication")) FabricationArmorCompat.touch();
		ModfestCardinalsCompat.touch(); // Does its own checks

		LOGGER.info("Switchy: Initialized! Already Registered Modules: " + COMPAT_REGISTRY.keySet());
	}
}

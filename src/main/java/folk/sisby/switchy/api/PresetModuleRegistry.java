package folk.sisby.switchy.api;

import folk.sisby.switchy.Switchy;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import static folk.sisby.switchy.util.Feedback.translatable;

public class PresetModuleRegistry {
	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable) {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, Set.of(), translatable("commands.switchy.module.disable.warn"));
	}

	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies) {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, applyDependencies, translatable("commands.switchy.module.disable.warn"));
	}

	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies, MutableText disableConfirmation) {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, applyDependencies, disableConfirmation);
	}
}

package folk.sisby.switchy.api;

import folk.sisby.switchy.Switchy;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import static folk.sisby.switchy.util.Feedback.translatable;

public class PresetModuleRegistry {
	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable) throws IllegalArgumentException, IllegalStateException  {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, Set.of(), Set.of(), translatable("commands.switchy.module.disable.warn"));
	}

	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies) throws IllegalArgumentException, IllegalStateException  {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, applyDependencies, Set.of(), translatable("commands.switchy.module.disable.warn"));
	}

	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds) throws IllegalArgumentException, IllegalStateException  {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, applyDependencies, uniqueIds, translatable("commands.switchy.module.disable.warn"));
	}

	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies, MutableText disableConfirmation) throws IllegalArgumentException, IllegalStateException  {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, applyDependencies, Set.of(), disableConfirmation);
	}

	public static void registerModule(Identifier moduleId, Supplier<PresetModule> moduleConstructor, boolean isDefault, ModuleImportable importable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) throws IllegalArgumentException, IllegalStateException  {
		Switchy.registerModule(moduleId, moduleConstructor, isDefault, importable, applyDependencies, uniqueIds, disableConfirmation);
	}
}

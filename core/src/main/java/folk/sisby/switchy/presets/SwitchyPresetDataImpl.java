package folk.sisby.switchy.presets;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.exception.ClassNotAssignableException;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.presets.SwitchyPresetData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Sisby folk
 * @see SwitchyPresetData
 * @since 2.0.0
 */
public class SwitchyPresetDataImpl<Module extends SwitchySerializable> implements SwitchyPresetData<Module> {
	private final Map<Identifier, Module> modules;
	private String name;

	/**
	 * Constructs an instance of the object.
	 *
	 * @param name           the desired name for the new preset.
	 * @param modules        the enabled status of modules from the presets object.
	 * @param moduleSupplier a function to supply module instances from their ID, usually from a registry.
	 * @throws InvalidWordException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 */
	public SwitchyPresetDataImpl(String name, Map<Identifier, Boolean> modules, Function<Identifier, Module> moduleSupplier) throws InvalidWordException {
		setName(name);
		Map<Identifier, Module> suppliedModules = new HashMap<>();
		modules.forEach((id, enabled) -> {
			if (enabled) {
				Module module = moduleSupplier.apply(id);
				if (module != null) suppliedModules.put(id, moduleSupplier.apply(id));
			}
		});
		this.modules = suppliedModules;
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		modules.forEach((id, module) -> outNbt.put(id.toString(), module.toNbt()));
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		modules.forEach((id, module) -> module.fillFromNbt(nbt.getCompound(id.toString())));
	}

	@Override
	@ApiStatus.Internal
	public Map<Identifier, Module> getModules() {
		return modules;
	}

	@Override
	@ApiStatus.Internal
	public Module getModule(Identifier id) throws ModuleNotFoundException {
		if (!containsModule(id)) throw new ModuleNotFoundException();
		return modules.get(id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ModuleType extends Module> ModuleType getModule(Identifier id, Class<ModuleType> clazz) throws ModuleNotFoundException, ClassNotAssignableException {
		Module module = getModule(id);
		if (!clazz.isAssignableFrom(module.getClass()))
			throw new ClassNotAssignableException("Module '" + id.toString(), module, clazz);
		return (ModuleType) module;
	}

	@Override
	@ApiStatus.Internal
	public void putModule(Identifier id, Module module) {
		modules.put(id, module);
	}

	@Override
	public boolean containsModule(Identifier id) {
		return modules.containsKey(id);
	}

	@Override
	public void removeModule(Identifier id) {
		if (!containsModule(id)) throw new ModuleNotFoundException();
		modules.remove(id);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) throws InvalidWordException {
		if (!name.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString))
			throw new InvalidWordException();
		this.name = name;
	}

	@Override
	public String toString() {
		return getName();
	}
}

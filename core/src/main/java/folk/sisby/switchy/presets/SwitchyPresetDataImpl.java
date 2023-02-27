package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
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
	 * @param name           the desired name for the new preset.
	 * @param modules        the enabled status of modules from the presets object
	 * @param moduleSupplier a function to supply module instances from their ID, usually from a registry.
	 */
	public SwitchyPresetDataImpl(String name, Map<Identifier, Boolean> modules, Function<Identifier, Module> moduleSupplier) {
		this.name = name;
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

	// Modules Accessors

	@Override
	@ApiStatus.Internal
	public Map<Identifier, Module> getModules() {
		return modules;
	}

	@Override
	@ApiStatus.Internal
	public Module getModule(Identifier id) {
		return modules.get(id);
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
		modules.remove(id);
	}

	// Name Accessors

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getName();
	}
}

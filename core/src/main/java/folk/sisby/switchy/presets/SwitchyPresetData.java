package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SwitchyPresetData<Module extends SwitchySerializable> implements SwitchySerializable {
	private final Map<Identifier, Module> modules;
	private String name;

	public SwitchyPresetData(String name, Map<Identifier, Boolean> modules, Function<Identifier, Module> moduleSupplier) {
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

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		modules.forEach((id, module) -> outNbt.put(id.toString(), module.toNbt()));
		return outNbt;
	}

	public void fillFromNbt(NbtCompound nbt) {
		modules.forEach((id, module) -> module.fillFromNbt(nbt.getCompound(id.toString())));
	}

	// Modules Accessors

	@ApiStatus.Internal
	public Map<Identifier, Module> getModules() {
		return modules;
	}

	@ApiStatus.Internal
	public Module getModule(Identifier id) {
		return modules.get(id);
	}

	@ApiStatus.Internal
	public void putModule(Identifier id, Module module) {
		modules.put(id, module);
	}

	public boolean containsModule(Identifier id) {
		return modules.containsKey(id);
	}

	public void removeModule(Identifier id) {
		modules.remove(id);
	}

	// Name Accessors

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getName();
	}
}

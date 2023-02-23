package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SwitchyPresetData<Module extends SwitchySerializable> implements SwitchySerializable {
	public final Map<Identifier, Module> modules;
	public String name;

	public SwitchyPresetData(String name, Map<Identifier, Boolean> modules, Map<Identifier, Supplier<Module>> moduleSupplier) {
		this.name = name;
		this.modules = modules.entrySet().stream().filter(Map.Entry::getValue).collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> moduleSupplier.get(e.getKey()).get()
		));
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		this.modules.forEach((id, module) -> outNbt.put(id.toString(), module.toNbt()));
		return outNbt;
	}

	public void fillFromNbt(NbtCompound nbt) {
		modules.forEach((id, module) -> module.fillFromNbt(nbt.getCompound(id.toString())));
	}

	@Override
	public String toString() {
		return name;
	}
}

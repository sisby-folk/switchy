package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Map;

public class SwitchyPresetData<Module extends SwitchySerializable> implements SwitchySerializable {
	public final Map<Identifier, Module> modules;

	public String presetName;

	public SwitchyPresetData(String name, Map<Identifier, Module> modules) {
		this.presetName = name;
		this.modules = modules;
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
		return presetName;
	}
}

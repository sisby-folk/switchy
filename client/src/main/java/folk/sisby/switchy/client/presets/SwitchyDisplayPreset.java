package folk.sisby.switchy.client.presets;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SwitchyDisplayPreset {
	public final String presetName;
	public final Map<Identifier, NbtCompound> modules;

	public SwitchyDisplayPreset(String presetName, Map<Identifier, NbtCompound> modules) {
		this.presetName = presetName;
		this.modules = modules;
	}

	public static SwitchyDisplayPreset fromNbt(String presetName, NbtCompound nbt, Collection<Identifier> moduleIds) {
		return new SwitchyDisplayPreset(presetName, moduleIds.stream().collect(Collectors.toMap(id -> id, id -> nbt.getCompound(id.toString()))));
	}
}

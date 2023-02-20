package folk.sisby.switchy.presets;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.SwitchyDisplayModules;
import folk.sisby.switchy.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.api.module.SwitchyModuleData;
import folk.sisby.switchy.client.screen.SwitchScreen;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SwitchyDisplayPreset {
	public final String presetName;
	public final Map<Identifier, SwitchyDisplayModule<? extends SwitchyModuleData>> modules;

	public SwitchyDisplayPreset(String presetName, Collection<Identifier> modules) {
		this.presetName = presetName;
		this.modules = modules.stream()
				.filter(SwitchyDisplayModules.MODULE_SUPPLIERS::containsKey)
				.collect(Collectors.toMap(id -> id, id -> SwitchyDisplayModules.MODULE_SUPPLIERS.get(id).get()));
	}

	public Map<Identifier, Pair<Component, SwitchScreen.ComponentPosition>> getDisplayComponents() {
		return modules.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getDisplayComponent()
				));
	}

	public static SwitchyDisplayPreset fromNbt(String presetName, NbtCompound nbt, Collection<Identifier> modules) {
		SwitchyDisplayPreset outPreset = new SwitchyDisplayPreset(presetName, modules);
		outPreset.modules.forEach((id, module) -> module.fillFromNbt(nbt.getCompound(id.toString())));
		return outPreset;
	}

	public static SwitchyDisplayPreset fromPreset(SwitchyPreset preset) {
		SwitchyDisplayPreset outPreset = new SwitchyDisplayPreset(preset.presetName, preset.modules.keySet());
		// TODO: outPreset.modules.forEach((id, module) -> module.fillFromData(preset.modules.get(id)));
		return outPreset;
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		this.modules.forEach((id, module) -> outNbt.put(id.toString(), module.toNbt()));
		return outNbt;
	}
}

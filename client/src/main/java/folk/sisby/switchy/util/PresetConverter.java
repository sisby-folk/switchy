package folk.sisby.switchy.util;

import folk.sisby.switchy.api.module.SwitchyModuleDisplayable;
import folk.sisby.switchy.presets.SwitchyPreset;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import static folk.sisby.switchy.presets.SwitchyPresets.*;

public class PresetConverter {
	public static NbtCompound presetsToNbt(SwitchyPresets presets) {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		presets.getModules().forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound listNbt = new NbtCompound();
		presets.getPresets().forEach((name, preset) -> listNbt.put(name, presetToNbt(preset)));
		outNbt.put(KEY_PRESET_LIST, listNbt);

		outNbt.putString(KEY_PRESET_CURRENT, presets.getCurrentPresetName());
		return outNbt;
	}

	public static NbtCompound presetToNbt(SwitchyPreset preset) {
		NbtCompound outNbt = new NbtCompound();
		preset.getModules().forEach((id, module) -> {
			if (module instanceof SwitchyModuleDisplayable displayableModule) {
				outNbt.put(id.toString(), displayableModule.toDisplayNbt());
			}
		});
		return outNbt;
	}
}

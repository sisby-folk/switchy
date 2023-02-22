package folk.sisby.switchy.util;

import folk.sisby.switchy.api.module.SwitchyModuleDisplayable;
import folk.sisby.switchy.presets.SwitchyPreset;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import static folk.sisby.switchy.presets.SwitchyPresets.*;

public class PresetDisplayNbt {
	public static NbtCompound presetsToNbt(SwitchyPresets presets) {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		presets.modules.forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound listNbt = new NbtCompound();
		for (SwitchyPreset preset : presets.presets.values()) {
			listNbt.put(preset.presetName, presetToNbt(preset));
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		if (presets.currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, presets.currentPreset.presetName);
		return outNbt;
	}

	public static NbtCompound presetToNbt(SwitchyPreset preset) {
		NbtCompound outNbt = new NbtCompound();
		preset.modules.forEach((id, module) -> {
			if (module instanceof SwitchyModuleDisplayable displayableModule) {
				outNbt.put(id.toString(), displayableModule.toDisplayNbt());
			}
		});
		return outNbt;
	}
}

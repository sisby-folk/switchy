package folk.sisby.switchy.util;

import folk.sisby.switchy.api.module.SwitchyModuleDisplayable;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

/**
 * @author Sisby folk
 * @since 2.0.0
 * Used to convert SwitchyPresets objects into NBT data usable by SwitchyDisplayPresets
 */
public class PresetConverter {
	// Figure out how to add this to a file or something. Mixin feels wrong but maybe.

	public static NbtCompound presetsToNbt(SwitchyPresets presets) {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		presets.getModules().forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(SwitchyPresetsData.KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound listNbt = new NbtCompound();
		presets.getPresets().forEach((name, preset) -> listNbt.put(name, presetToNbt(preset)));
		outNbt.put(SwitchyPresetsData.KEY_PRESET_LIST, listNbt);

		outNbt.putString(SwitchyPresetsData.KEY_PRESET_CURRENT, presets.getCurrentPresetName());
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

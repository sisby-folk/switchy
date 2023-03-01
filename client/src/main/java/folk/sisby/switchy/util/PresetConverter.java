package folk.sisby.switchy.util;

import folk.sisby.switchy.api.module.SwitchyModuleDisplayable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

/**
 * Used to convert SwitchyPresets objects into NBT data usable by SwitchyDisplayPresets.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class PresetConverter {
	/**
	 * The NBT key where module info should be stored.
	 */
	public static final String KEY_MODULE_INFO = "moduleInfo";
	// Figure out how to add this to a file or something. Mixin feels wrong but maybe.

	/**
	 * @param presets an arbitrary presets object.
	 * @return its serialized displayable representation to be used in {@link folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets#fillFromNbt(NbtCompound)}.
	 */
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

		outNbt.put(KEY_MODULE_INFO,SwitchyModuleRegistry.infoToNbt());

		return outNbt;
	}

	/**
	 * @param preset an arbitrary preset object.
	 * @return its serialized displayable representation to be used in {@link folk.sisby.switchy.api.module.presets.SwitchyDisplayPreset#fillFromNbt(NbtCompound)}.
	 */
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

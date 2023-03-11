package folk.sisby.switchy.util;

import folk.sisby.switchy.api.module.SwitchyModuleTransferable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.module.presets.SwitchyClientPreset;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Used to convert SwitchyPresets objects into NBT data usable by SwitchyClientPresets.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class PresetConverter {
	/**
	 * The NBT key where module info should be stored.
	 */
	public static final String KEY_MODULE_INFO = "moduleInfo";
	/**
	 * The NBT key where permission level should be stored.
	 */
	public static final String KEY_PERMISSION_LEVEL = "permissionLevel";
	// Figure out how to add this to a file or something. Mixin feels wrong but maybe.

	/**
	 * @param player the relevant player.
	 * @param presets an arbitrary presets object.
	 * @return its serialized client-compatible representation to be used in {@link SwitchyClientPresets#fillFromNbt(NbtCompound)}.
	 */
	public static NbtCompound presetsToNbt(ServerPlayerEntity player, SwitchyPresets presets) {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		presets.getModules().forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(SwitchyPresetsData.KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound presetsCompound = new NbtCompound();
		presets.getPresets().forEach((name, preset) -> presetsCompound.put(name, presetToNbt(preset)));
		outNbt.put(SwitchyPresetsData.KEY_PRESETS, presetsCompound);

		outNbt.putString(SwitchyPresetsData.KEY_PRESET_CURRENT, presets.getCurrentPresetName());

		outNbt.put(KEY_MODULE_INFO,SwitchyModuleRegistry.infoToNbt(player));
		outNbt.putInt(KEY_PERMISSION_LEVEL, player.server.getPermissionLevel(player.getGameProfile()));

		return outNbt;
	}

	/**
	 * @param preset an arbitrary preset object.
	 * @return its serialized client-compatible representation to be used in {@link SwitchyClientPreset#fillFromNbt(NbtCompound)}.
	 */
	public static NbtCompound presetToNbt(SwitchyPreset preset) {
		NbtCompound outNbt = new NbtCompound();
		preset.getModules().forEach((id, module) -> {
			if (module instanceof SwitchyModuleTransferable transferableModule) {
				outNbt.put(id.toString(), transferableModule.toClientNbt());
			}
		});
		return outNbt;
	}
}

package folk.sisby.switchy.api;

import folk.sisby.switchy.SwitchyPresets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerPresets {
	public static List<String> getPlayerPresetNames(PlayerEntity player) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		return presets != null ? presets.getPresetNames() : List.of();
	}

	public static String getPlayerCurrentPresetName(PlayerEntity player) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		return presets != null ? presets.getCurrentPreset().toString() : "";
	}

	public static boolean switchPlayerPreset(PlayerEntity player, String presetName) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		return presets != null && presets.setCurrentPreset(player, presetName, true);
	}

	public static Map<Identifier, Boolean> getPlayerPresetModules(PlayerEntity player) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		return presets != null ? presets.modules : new HashMap<>();
	}
}

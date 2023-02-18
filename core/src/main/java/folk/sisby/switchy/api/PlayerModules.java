package folk.sisby.switchy.api;

import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PlayerModules {
	/**
	 * Gets a map of every instance of the specified module by its preset name, or empty if its disabled.
	 **/
	public static Map<String, PresetModule> getAllOfModule(PlayerEntity player, Identifier moduleId) throws IllegalArgumentException, IllegalStateException {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		if (presets == null) throw new IllegalStateException("Specified player has no presets");
		if (!presets.modules.containsKey(moduleId)) throw new IllegalArgumentException("Specified module does not exist");
		Map<String, PresetModule> outMap = new HashMap<>();
		if (presets.modules.get(moduleId)) {
			presets.presets.forEach((name, preset) -> {
				outMap.put(name, preset.modules.get(moduleId));
			});
		}
		return outMap;
	}

	/**
	 * Allows you to modify the data associated with the current preset for a specified module, by saving it, mutating it, then loading it all in one swoop.
	 **/
	public static void duckCurrentModule(PlayerEntity player, Identifier moduleId, Consumer<PresetModule> mutator) throws IllegalArgumentException, IllegalStateException  {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		if (presets == null) throw new IllegalStateException("Specified player has no presets");
		presets.duckCurrentModule(player, moduleId, mutator);
	}
}

package folk.sisby.switchy.api.module.presets;

import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * @author Sisby folk
 * @see SwitchyDisplayPreset
 * @see folk.sisby.switchy.api.presets.SwitchyPresets
 * A collection of {@link SwitchyDisplayPreset}s, representing a player's {@link folk.sisby.switchy.api.presets.SwitchyPresets} from the server.
 * @since 2.0.0
 */
public interface SwitchyDisplayPresets extends SwitchyPresetsData<SwitchyDisplayModule, SwitchyDisplayPreset> {
	/**
	 * Gets the module info for all modules.
	 * @return a map of module info by module ID.
	 */
	Map<Identifier, SwitchyModuleInfo> getModuleInfo();
}

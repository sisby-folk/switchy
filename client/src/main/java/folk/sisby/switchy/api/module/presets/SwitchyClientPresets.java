package folk.sisby.switchy.api.module.presets;

import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * A collection of {@link SwitchyClientPreset}s, representing a player's {@link folk.sisby.switchy.api.presets.SwitchyPresets} from the server.
 *
 * @author Sisby folk
 * @see SwitchyClientPreset
 * @see folk.sisby.switchy.api.presets.SwitchyPresets
 * @since 2.0.0
 */
public interface SwitchyClientPresets extends SwitchyPresetsData<SwitchyClientModule, SwitchyClientPreset> {
	/**
	 * Gets the module info for all modules.
	 *
	 * @return a map of module info by module ID.
	 */
	Map<Identifier, SwitchyModuleInfo> getModuleInfo();

	/**
	 * Get the known permission level.
	 *
	 * @return the permission level of the client player.
	 */
	int getPermissionLevel();
}

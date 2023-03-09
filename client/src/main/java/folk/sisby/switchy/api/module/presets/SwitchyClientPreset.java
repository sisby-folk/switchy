package folk.sisby.switchy.api.module.presets;

import folk.sisby.switchy.api.presets.SwitchyPresetData;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;

/**
 * @author Sisby folk
 * @see SwitchyClientModule
 * @see folk.sisby.switchy.api.presets.SwitchyPreset
 * A named collection of {@link SwitchyClientModule}s, representing a {@link folk.sisby.switchy.api.presets.SwitchyPreset} from the server.
 * @since 2.0.0
 */
public interface SwitchyClientPreset extends SwitchyPresetData<SwitchyClientModule> {
}

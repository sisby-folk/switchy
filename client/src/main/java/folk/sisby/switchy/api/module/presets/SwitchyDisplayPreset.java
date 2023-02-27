package folk.sisby.switchy.api.module.presets;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.presets.SwitchyPresetData;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * @author Sisby folk
 * @since 2.0.0
 * @see SwitchyDisplayModule
 * @see folk.sisby.switchy.api.presets.SwitchyPreset
 * A named collection of {@link SwitchyDisplayModule}s, representing a {@link folk.sisby.switchy.api.presets.SwitchyPreset} from the server.
 */
public interface SwitchyDisplayPreset extends SwitchyPresetData<SwitchyDisplayModule> {
	/**
	 * @return all displayable components representing the preset's modules, by module ID.
	 */
	Map<Identifier, Pair<Component, SwitchySwitchScreenPosition>> getDisplayComponents();
}

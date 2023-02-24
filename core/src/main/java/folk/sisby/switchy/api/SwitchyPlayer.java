package folk.sisby.switchy.api;

import folk.sisby.switchy.api.presets.SwitchyPresets;
import org.jetbrains.annotations.ApiStatus;

public interface SwitchyPlayer {
	@ApiStatus.Internal
	void switchy$setPresets(SwitchyPresets presets);

	SwitchyPresets switchy$getPresets();
}

package folk.sisby.switchy.api;

import folk.sisby.switchy.presets.SwitchyPresets;

public interface SwitchyPlayer {

	void switchy$setPresets(SwitchyPresets presets);

	SwitchyPresets switchy$getPresets();

}

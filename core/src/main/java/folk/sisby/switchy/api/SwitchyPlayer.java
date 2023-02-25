package folk.sisby.switchy.api;

import folk.sisby.switchy.api.presets.SwitchyPresets;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Sisby folk
 * @since 1.0.0
 * A {@link net.minecraft.server.network.ServerPlayerEntity} mixin-implemented interface for storing the per-player Presets object.
 */
public interface SwitchyPlayer {
	@ApiStatus.Internal
	void switchy$setPresets(SwitchyPresets presets);

	SwitchyPresets switchy$getPresets();
}

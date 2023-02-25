package folk.sisby.switchy.api;

import folk.sisby.switchy.api.presets.SwitchyPresets;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Sisby folk
 * @since 1.0.0
 * A {@link net.minecraft.server.network.ServerPlayerEntity} mixin-implemented interface for storing the per-player Presets object.
 */
public interface SwitchyPlayer {
	/**
	 * @param presets a new presets object for the player
	 */
	@ApiStatus.Internal
	void switchy$setPresets(SwitchyPresets presets);

	/**
	 * @return the presets object for the player.
	 */
	SwitchyPresets switchy$getPresets();
}

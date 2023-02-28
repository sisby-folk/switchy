package folk.sisby.switchy.api;

import folk.sisby.switchy.api.presets.SwitchyPresets;
import org.jetbrains.annotations.ApiStatus;

/**
 * A {@link net.minecraft.server.network.ServerPlayerEntity}-mixin-implemented interface for storing the per-player Presets object.
 *
 * @author Sisby folk
 * @since 1.0.0
 */
public interface SwitchyPlayer {
	/**
	 * Set the presets object associated with the player.
	 *
	 * @param presets a new presets object for the player.
	 */
	@ApiStatus.Internal
	void switchy$setPresets(SwitchyPresets presets);

	/**
	 * Gets the presets object associated with this player.
	 *
	 * @return the presets object for the player.
	 */
	SwitchyPresets switchy$getPresets();
}

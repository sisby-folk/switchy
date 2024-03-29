package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.SwitchyApplicable;
import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A bundle of copied player data (defined by {@link SwitchyModule}s) capable of being updated from and applied to a player.
 *
 * @author Sisby folk
 * @see SwitchyPresetData
 * @see SwitchyApplicable
 * @since 1.0.0
 */
public interface SwitchyPreset extends SwitchyPresetData<SwitchyModule>, SwitchyApplicable<ServerPlayerEntity> {
}

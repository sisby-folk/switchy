package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.SwitchyApplicable;
import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Sisby folk
 * @since 1.0.0
 * @see SwitchyPresetData
 * @see SwitchyApplicable
 * A bundle of copied player data (defined by {@link SwitchyModule}s) capable of being updated from and applied to a player.
 */
public interface SwitchyPreset extends SwitchyPresetData<SwitchyModule>, SwitchyApplicable<ServerPlayerEntity> {
}

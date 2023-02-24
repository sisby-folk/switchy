package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;

public interface SwitchyPreset extends SwitchyPresetData<SwitchyModule> {
	void updateFromPlayer(ServerPlayerEntity player, String nextPreset);

	void applyToPlayer(ServerPlayerEntity player);
}

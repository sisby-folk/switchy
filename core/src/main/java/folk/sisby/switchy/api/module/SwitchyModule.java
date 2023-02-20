package folk.sisby.switchy.api.module;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface SwitchyModule extends SwitchyModuleData {
	void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset);

	void applyToPlayer(ServerPlayerEntity player);
}

package folk.sisby.switchy.api.module;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface SwitchyModule extends SwitchySerializable {
	void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset);

	void applyToPlayer(ServerPlayerEntity player);
}

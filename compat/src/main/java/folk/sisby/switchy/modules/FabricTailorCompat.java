package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.module.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.fabrictailor.casts.TailoredPlayer;

public class FabricTailorCompat extends FabricTailorCompatData implements SwitchyModule, SwitchyModuleDisplayable {
	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		skinValue = tailoredPlayer.getSkinValue();
		skinSignature = tailoredPlayer.getSkinSignature();
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		if (skinValue != null && skinSignature != null) {
			tailoredPlayer.setSkin(skinValue, skinSignature, true);
		}
	}

	@Override
	public NbtCompound toDisplayNbt() {
		return toNbt();
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		SwitchyModuleRegistry.registerModule(ID, FabricTailorCompat::new, new SwitchyModuleInfo(true, SwitchyModuleEditable.ALWAYS_ALLOWED));
	}
}

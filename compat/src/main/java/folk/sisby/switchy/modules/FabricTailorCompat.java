package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.fabrictailor.casts.TailoredPlayer;

public class FabricTailorCompat extends FabricTailorCompatData implements SwitchyModule {
	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		this.skinValue = tailoredPlayer.getSkinValue();
		this.skinSignature = tailoredPlayer.getSkinSignature();
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		if (this.skinValue != null && this.skinSignature != null) {
			tailoredPlayer.setSkin(this.skinValue, this.skinSignature, true);
		}
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		SwitchyModuleRegistry.registerModule(ID, FabricTailorCompat::new, true, SwitchyModuleEditable.ALWAYS_ALLOWED);
		FabricTailorCompatDisplay.touch();
	}
}

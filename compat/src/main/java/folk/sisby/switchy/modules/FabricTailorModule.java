package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.module.*;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.fabrictailor.casts.TailoredPlayer;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches player skins from samolego's Fabric Tailor.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @see FabricTailorModuleData
 * @since 1.0.0
 */
public class FabricTailorModule extends FabricTailorModuleData implements SwitchyModule, SwitchyModuleTransferable {
	static {
		SwitchyModuleRegistry.registerModule(ID, FabricTailorModule::new, new SwitchyModuleInfo(
						true,
						SwitchyModuleEditable.ALWAYS_ALLOWED,
						translatable("switchy.compat.module.fabrictailor.description")
				)
						.withDescriptionWhenEnabled(translatable("switchy.compat.module.fabrictailor.enabled"))
						.withDescriptionWhenDisabled(translatable("switchy.compat.module.fabrictailor.disabled"))
						.withDeletionWarning(translatable("switchy.compat.module.fabrictailor.warning"))
		);
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

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
}

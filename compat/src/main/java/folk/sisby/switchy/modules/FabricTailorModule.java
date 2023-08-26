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
	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyModuleRegistry.registerModule(ID, FabricTailorModule::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.ALWAYS_ALLOWED,
				translatable("switchy.modules.switchy.fabrictailor.description")
			)
				.withDescriptionWhenEnabled(translatable("switchy.modules.switchy.fabrictailor.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.modules.switchy.fabrictailor.disabled"))
				.withDeletionWarning(translatable("switchy.modules.switchy.fabrictailor.warning"))
		);
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

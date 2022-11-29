package folk.sisby.switchy.mixin.compat.styled_nicknames;

import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConfigManager.class, remap = false)
public abstract class ConfigManagerMixin {
	@Shadow private static Config CONFIG;

	@Inject(method = "getConfig", at = @At("RETURN"))
	private static void forceAllowByDefault(CallbackInfoReturnable<Config> cir) {
		CONFIG.configData.allowByDefault = true;
	}
}

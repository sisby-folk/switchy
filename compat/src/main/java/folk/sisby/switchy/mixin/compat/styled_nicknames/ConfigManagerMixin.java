package folk.sisby.switchy.mixin.compat.styled_nicknames;

import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Modifies Styled Nicknames to allow nicknames to be self-assigned by any player, with colour and tooltips.
 *
 * @author Sisby folk
 * @since 1.7.2
 */
@Mixin(value = ConfigManager.class, remap = false)
public abstract class ConfigManagerMixin {
	@Shadow private static Config CONFIG;

	@Inject(method = "getConfig", at = @At("RETURN"))
	private static void forceAllowByDefault(CallbackInfoReturnable<Config> cir) {
		CONFIG.configData.allowByDefault = true;
		CONFIG.configData.defaultEnabledFormatting.put("color", true); // For SwitchyKit
		CONFIG.configData.defaultEnabledFormatting.put("hover", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_red", true);
		CONFIG.configData.defaultEnabledFormatting.put("yellow", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_blue", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_purple", true);
		CONFIG.configData.defaultEnabledFormatting.put("gold", true);
		CONFIG.configData.defaultEnabledFormatting.put("red", true);
		CONFIG.configData.defaultEnabledFormatting.put("aqua", true);
		CONFIG.configData.defaultEnabledFormatting.put("gray", true);
		CONFIG.configData.defaultEnabledFormatting.put("light_purple", true);
		CONFIG.configData.defaultEnabledFormatting.put("white", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_gray", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_grey", true);
		CONFIG.configData.defaultEnabledFormatting.put("green", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_green", true);
		// CONFIG.configData.defaultEnabledFormatting.put("black", true); Drogtor's Choice
		CONFIG.configData.defaultEnabledFormatting.put("grey", true);
		CONFIG.configData.defaultEnabledFormatting.put("orange", true);
		CONFIG.configData.defaultEnabledFormatting.put("blue", true);
		CONFIG.configData.defaultEnabledFormatting.put("dark_aqua", true);
	}
}

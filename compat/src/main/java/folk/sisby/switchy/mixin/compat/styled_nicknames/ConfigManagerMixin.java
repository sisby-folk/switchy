package folk.sisby.switchy.mixin.compat.styled_nicknames;

import eu.pb4.stylednicknames.config.ConfigManager;
import eu.pb4.stylednicknames.config.data.ConfigData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Modifies Styled Nicknames to allow nicknames to be self-assigned by any player by default.
 * Fixes an incongruity between import permissions for the module and standard permissions for the mod,
 * effectively preventing importing nickname modules from being considered an "exploit" for changing name.
 *
 * @author Sisby folk
 * @since 1.7.2
 */
@Mixin(value = ConfigManager.class, remap = false)
public abstract class ConfigManagerMixin {
	@ModifyVariable(method = "loadConfig", at = @At(value = "INVOKE", target = "Ljava/io/BufferedWriter;<init>(Ljava/io/Writer;)V"))
	private static ConfigData forceDefaultEnabledColorHover(ConfigData configData) {
		configData.allowByDefault = true;
		return configData;
	}
}

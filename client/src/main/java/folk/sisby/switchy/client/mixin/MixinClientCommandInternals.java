package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import org.quiltmc.qsl.command.impl.client.ClientCommandInternals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Garden System
 * @since 2.0.0
 * @see SwitchyClientCommands
 * Records client commands after they're executed into a map of previous commands per player UUID.
 */
@Mixin(value = ClientCommandInternals.class, remap = false)
public abstract class MixinClientCommandInternals {

	@Inject(at = @At("RETURN"), method = "executeCommand")
	private static void executeCommand(String message, boolean ignorePrefix, CallbackInfoReturnable<Boolean> cir)
	{
		SwitchyClientCommands.HISTORY = ignorePrefix ? message : message.substring(1);
	}
}

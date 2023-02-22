package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import org.quiltmc.qsl.command.impl.client.ClientCommandInternals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientCommandInternals.class, remap = false)
public abstract class MixinClientCommandInternals {

	@Inject(at = @At("RETURN"), method = "executeCommand")
	private static void executeCommand(String message, boolean ignorePrefix, CallbackInfoReturnable<Boolean> cir)
	{
		SwitchyClientCommands.HISTORY = ignorePrefix ? message : message.substring(1);
	}
}

package folk.sisby.switchy.mixin;

import folk.sisby.switchy.SwitchyCommands;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records commands after they're executed into a map of previous commands per player UUID.
 *
 * @author Garden System
 * @see SwitchyCommands
 * @since 2.0.0
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {
	@SuppressWarnings("DataFlowIssue")
	@Inject(at = @At(value = "TAIL"), method = "executeCommand")
	void onChatCommand(String input, CallbackInfo ci) {
		SwitchyCommands.HISTORY.put(((ServerPlayNetworkHandler) (Object) this).player.getUuid(), input);
	}
}

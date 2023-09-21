package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records client player commands before they're sent for confirmations.
 *
 * @author Garden System
 * @see SwitchyClientCommands
 * @since 2.0.0
 */
@Mixin(value = ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {
	@Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"))
	private void recordCommandHistory(String string, CallbackInfo ci) {
		SwitchyClientCommands.HISTORY = string;
	}
}

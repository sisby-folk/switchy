package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records player commands after they're executed into a map of previous commands per player UUID.
 *
 * @author Garden System
 * @see SwitchyClientCommands
 * @since 2.0.0
 */
@Mixin(value = ClientPlayerEntity.class, remap = false)
public abstract class MixinClientPlayerEntity {
	@Inject(method = "method_43787", at = @At("HEAD"))
	private void recordCommandHistory(String string, Text text, CallbackInfo ci) {
		SwitchyClientCommands.HISTORY = string;
	}
}

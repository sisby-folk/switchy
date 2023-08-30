package folk.sisby.switchy.ui.mixin.compat.owo_lib;

import folk.sisby.switchy.ui.util.SwitchyOwoUtil;
import io.wispforest.owo.ui.core.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Component.class, remap = false)
public interface MixinComponent {
	@Inject(method = "isInBoundingBox", at = @At("RETURN"), cancellable = true)
	default void dontHoverUnderOverlays(double x, double y, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) cir.setReturnValue(SwitchyOwoUtil.componentCanHover((Component) this));
	}
}

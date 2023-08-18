package folk.sisby.switchy.mixin.compat.figura;

import folk.sisby.switchy.client.SwitchyFiguraApi;
import org.figuramc.figura.avatar.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Avatar.class)
public class AvatarMixin {
	@Inject(method = "clean", at = @At("HEAD"))
	public void clean(CallbackInfo ci) {
		Avatar self = (Avatar) (Object) this;
		SwitchyFiguraApi.AVATAR_LISTENERS.remove(self);
	}
}

package folk.sisby.switchy.ui.mixin.compat.owo_lib;

import folk.sisby.switchy.ui.util.SwitchyOwoUtil;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public class MixinClickableWidget {
	@Shadow protected boolean hovered;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ClickableWidget;renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
	public void dontHoverUnderOverlays(DrawContext drawContext, int i, int j, float f, CallbackInfo ci) {
		if (hovered) hovered = SwitchyOwoUtil.componentCanHover((Component) this);
	}
}

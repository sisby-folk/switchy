package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;

public class LoadingOverlayComponent extends OverlayComponent<LabelComponent> {
	public LoadingOverlayComponent() {
		super(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, Components.label(Text.of("Loading...")));
	}
}

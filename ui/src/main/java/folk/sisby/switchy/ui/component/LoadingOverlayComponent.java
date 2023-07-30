package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.VerticalFlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.text.Text;

public class LoadingOverlayComponent extends VerticalFlowLayout {
	public final LabelComponent loadingLabel = Components.label(Text.of("Loading..."));

	public LoadingOverlayComponent() {
		super(Sizing.fill(100), Sizing.fill(100));
		this.alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM);
		this.positioning(Positioning.absolute(0, 0));
		this.child(loadingLabel);
		this.mouseDown().subscribe((x, y, b) -> true); // eat all input
	}
}

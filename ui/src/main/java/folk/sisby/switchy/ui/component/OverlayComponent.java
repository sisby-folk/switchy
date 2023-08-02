package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.container.VerticalFlowLayout;
import io.wispforest.owo.ui.core.*;

import java.util.function.Consumer;

public class OverlayComponent<T extends Component> extends VerticalFlowLayout {
	public Consumer<Component> dismiss;
	public final T child;

	public OverlayComponent(HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, T child) {
		super(Sizing.fill(100), Sizing.fill(100));
		this.positioning(Positioning.absolute(0, 0));
		this.surface(Surface.VANILLA_TRANSLUCENT);
		this.mouseDown().subscribe((x, y, b) -> true); // eat all input
		this.zIndex(100);
		this.alignment(horizontalAlignment, verticalAlignment);
		this.child(child);
		this.child = child;
	}

	public void dismiss() {
		dismiss.accept(this);
	}

	public void setDismiss(Consumer<Component> dismiss) {
		this.dismiss = dismiss;
	}
}

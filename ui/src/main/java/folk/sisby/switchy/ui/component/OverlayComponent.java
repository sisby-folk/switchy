package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.VerticalAlignment;

import java.util.function.Consumer;

public class OverlayComponent<T extends Component> extends OverlayContainer<T> {
	public final T child;
	public Consumer<Component> dismiss;

	public OverlayComponent(HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, T child) {
		super(child);
		this.alignment(horizontalAlignment, verticalAlignment);
		this.zIndex(100);
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

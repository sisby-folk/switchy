package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;

public class LockableFlowLayout extends OverlayableFlowLayout {
	protected final LoadingOverlayComponent loadingOverlay = new LoadingOverlayComponent();

	public LockableFlowLayout(Sizing horizontalSizing, Sizing verticalSizing) {
		super(horizontalSizing, verticalSizing);
		this.surface(Surface.VANILLA_TRANSLUCENT);
		this.horizontalAlignment(HorizontalAlignment.CENTER);
		this.verticalAlignment(VerticalAlignment.CENTER);
		this.gap(2);
	}

	public void lock() {
		addOverlay(loadingOverlay);
	}

	public void unlock() {
		removeOverlay();
	}
}

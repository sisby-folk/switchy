package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;

public class OverlayableFlowLayout extends FlowLayout {
	public OverlayComponent<?> overlay;

	public OverlayableFlowLayout(Sizing horizontalSizing, Sizing verticalSizing) {
		super(horizontalSizing, verticalSizing, Algorithm.VERTICAL);
	}

	public void addOverlay(OverlayComponent<?> overlay) {
		removeOverlay();
		overlay.setDismiss(c -> removeOverlay());
		this.overlay = overlay;
		this.child(overlay);
	}

	public void removeOverlay() {
		if (overlay != null) {
			this.removeChild(overlay);
			overlay = null;
		}
	}
}

package folk.sisby.switchy.ui.util;

import folk.sisby.switchy.ui.component.OverlayComponent;
import folk.sisby.switchy.ui.component.OverlayableFlowLayout;
import io.wispforest.owo.ui.core.Component;

public class SwitchyOwoUtil {
	public static boolean componentCanHover(Component component) {
		Component parent = component;
		do {
			if (parent instanceof OverlayComponent) break;
			parent = parent.parent();
			if (parent instanceof OverlayableFlowLayout ofl) return ofl.overlay == null;
		} while (parent != null);
		return true;
	}
}

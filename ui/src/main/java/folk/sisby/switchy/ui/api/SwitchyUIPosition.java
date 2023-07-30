package folk.sisby.switchy.ui.api;

import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * Defines the placement of a component in the preset preview.
 * Used in the quick-switcher screen.
 *
 * @author Sisby folk
 * @see folk.sisby.switchy.ui.screen.SwitchScreen
 * @since 1.9.0
 */
@ClientOnly
public enum SwitchyUIPosition {
	/**
	 * On the left side of the overall horizontal flow.
	 */
	SIDE_LEFT,
	/**
	 * Within the centered, left aligned vertical flow inside the overall horizontal flow.
	 */
	LEFT,
	/**
	 * Within the centered, right aligned vertical flow inside the overall horizontal flow.
	 */
	RIGHT,
	/**
	 * Between RIGHT and SIDE_RIGHT, inside a grid designed for ItemComponents.
	 */
	GRID_RIGHT,
	/**
	 * On the right side of the overall horizontal flow.
	 */
	SIDE_RIGHT
}

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
	 * Used for the preset name.
	 */
	SIDE_LEFT,
	/**
	 * Within the centered, left aligned vertical flow inside the overall horizontal flow.
	 * Good for variable-size text like names.
	 */
	LEFT,
	/**
	 * Between RIGHT and SIDE_RIGHT, inside a grid.
	 * Good for ItemComponents with tooltips.
	 */
	GRID_RIGHT,
	/**
	 * On the right side of the overall horizontal flow.
	 * Good for large previews like entity renderers, or short text with tooltips.
	 */
	SIDE_RIGHT
}

package folk.sisby.switchy.client.api;

import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * @author Sisby folk
 * @see folk.sisby.switchy.client.screen.SwitchScreen
 * Defines the placement of a component in the horizontal flow used to preview presets in the quick-switcher screen.
 * @since 1.9.0
 */
@ClientOnly
public enum SwitchySwitchScreenPosition {
	/**
	 * On the left side of the overall horizontal flow
	 */
	SIDE_LEFT,
	/**
	 * Within the centered, left aligned vertical flow inside the overall horizontal flow
	 */
	LEFT,
	/**
	 * Within the centered, right aligned vertical flow inside the overall horizontal flow
	 */
	RIGHT,
	/**
	 * On the right side of the overall horizontal flow
	 */
	SIDE_RIGHT
}

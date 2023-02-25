package folk.sisby.switchy.api.module;

/**
 * @author Sisby folk
 * @since 1.7.1
 * Represents the "cold-editing" permissions for a {@link SwitchyModule}
 * "Cold-Editing" is the process of changing module data while it's not actively applied to the player.
 * This includes importing presets from the client, addons exposing editing features.
 */
public enum SwitchyModuleEditable {
	/**
	 * Can be imported/edited by any player
	 * This value is not configurable
	 */
	ALWAYS_ALLOWED,
	/**
	 * Can be imported/edited by any player
	 * This value will be configurable in {@link folk.sisby.switchy.SwitchyConfig}
	 */
	ALLOWED,
	/**
	 * Can be imported/edited by an operator (level 2) when explicitly specified
	 * This value will be configurable in {@link folk.sisby.switchy.SwitchyConfig}
	 */
	OPERATOR,
	/**
	 * Cannot be imported or edited for technical reasons
	 */
	NEVER
}

package folk.sisby.switchy.api.module;

import folk.sisby.switchy.api.SwitchyApplicable;
import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Used to hold snapshots of part of the player's data (inventory, health, mod data, etc.).
 * Capable of saving the data to NBT, restoring it from NBT, and restoring it to the player.
 *
 * @author Sisby folk
 * @see SwitchySerializable
 * @see SwitchyApplicable
 * @since 1.0.0
 */
public interface SwitchyModule extends SwitchySerializable, SwitchyApplicable<ServerPlayerEntity> {
	/**
	 * Runs when a module is instantiated due to being enabled, rather than in a new preset.
	 * Useful for defining different initial states between the two.
	 */
	default void onEnable() {}
}

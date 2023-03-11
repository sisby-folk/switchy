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
	 *
	 * @param player the relevant player.
	 */
	default void onEnable(ServerPlayerEntity player) {
	}

	/**
	 * Runs when a module is being deleted.
	 * Useful for clearing references or recovering data (dropping inventories etc.).
	 *
	 * @param player      the relevant player.
	 * @param fromDisable true if all instances of this module are being deleted at once.
	 */
	default void onDelete(ServerPlayerEntity player, boolean fromDisable) {
	}
}

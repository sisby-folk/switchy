package folk.sisby.switchy.api.module;

import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Sisby folk
 * @since 1.0.0
 * An serializable object capable of copying a part of {@code playerdata} and applying it back to the player.
 * Used to hold snapshots of part of the player's data (inventory, health, mod data, etc.), save them to NBT, and restore them from NBT and to the player later.
 */
public interface SwitchyModule extends SwitchySerializable {
	/**
	 * @param player the player to fill the module's data from
	 * @param nextPreset the name of the upcoming preset. Intended for special addon logic - null when saving without switching.
	 * Runs when the holding preset is being switched out.
	 * Also runs before exporting, importing, on save, and when being "hot modified" with {@link SwitchyPresets#duckCurrentModule(ServerPlayerEntity, Identifier, Consumer)}
	 */
	void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset);

	/**
	 * @param player the player to apply the module's data to
	 * Runs when the holding preset is being switched in.
	 * Also runs when being "hot modified" with {@link SwitchyPresets#duckCurrentModule(ServerPlayerEntity, Identifier, Consumer)} (e.g. when importing)
	 */
	void applyToPlayer(ServerPlayerEntity player);
}

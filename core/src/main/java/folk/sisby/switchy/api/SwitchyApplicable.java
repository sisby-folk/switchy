package folk.sisby.switchy.api;

import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Sisby folk
 * @since 2.0.0
 * An object capable of copying some Player data and applying it back later.
 */
public interface SwitchyApplicable<Player extends PlayerEntity> {
	/**
	 * @param player     the player to fill the object's data from
	 * @param nextPreset the name of the upcoming preset. Intended for special addon logic - null when saving without switching.
	 *                   In Switchy, runs when a preset is being switched out.
	 *                   Also runs before exporting, importing, on save, and when being "hot modified" with {@link SwitchyPresets#duckCurrentModule(ServerPlayerEntity, Identifier, Consumer)}
	 */
	void updateFromPlayer(Player player, @Nullable String nextPreset);

	/**
	 * @param player the player to apply the object's data to
	 *               In Switchy, runs when the holding preset is being switched in.
	 *               Also runs when being "hot modified" with {@link SwitchyPresets#duckCurrentModule(ServerPlayerEntity, Identifier, Consumer)} (e.g. when importing)
	 */
	void applyToPlayer(Player player);
}

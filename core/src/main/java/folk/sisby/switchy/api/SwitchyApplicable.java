package folk.sisby.switchy.api;

import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * An object capable of copying some Player data and applying it back later.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public interface SwitchyApplicable<Player extends PlayerEntity> {
	/**
	 * In Switchy, runs when a preset is being switched out.
	 * Also runs before exporting, importing, on save, and when being "hot modified" with {@link SwitchyPresets#mutateModule(ServerPlayerEntity, String, Identifier, Consumer, Class)}.
	 *
	 * @param player     the player to fill the object's data from.
	 * @param nextPreset the name of the upcoming preset. Intended for special addon logic - null when saving without switching.
	 */
	void updateFromPlayer(Player player, @Nullable String nextPreset);

	/**
	 * In Switchy, runs when the holding preset is being switched in.
	 * Also runs when being "hot modified" with {@link SwitchyPresets#mutateModule(ServerPlayerEntity, String, Identifier, Consumer, Class)} (e.g. when importing).
	 *
	 * @param player the player to apply the object's data to.
	 */
	void applyToPlayer(Player player);
}

package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Sisby folk
 * @see SwitchyPresetsData
 * @see folk.sisby.switchy.api.SwitchyPlayer
 * Handles all Switchy interactions for a specific player, and holds all their data.
 * You were probably looking for this class.
 * A collection of {@link SwitchyPreset}
 * @since 1.0.0
 */
public interface SwitchyPresets extends SwitchyPresetsData<SwitchyModule, SwitchyPreset> {
	/**
	 * {@inheritDoc}
	 *
	 * @param player the player this presets object belongs to
	 *               Will hot-modify the current preset using {@link SwitchyPresets#duckCurrentModule(ServerPlayerEntity, Identifier, Consumer)}
	 */
	void importFromOther(ServerPlayerEntity player, Map<String, SwitchyPreset> other);

	/**
	 * @param player the player this presets object belongs to
	 * @param other  another presets object to import from
	 * @see SwitchyPresets#importFromOther(ServerPlayerEntity, Map)
	 */
	void importFromOther(ServerPlayerEntity player, SwitchyPresets other);

	/**
	 * @param player the player this presets object belongs to
	 * @param name   the case-insensitive name of a preset
	 * @return the (case-corrected) name of the new current preset
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when the preset with the specified name is the current preset
	 *                                  "Switches" from the current preset to the specified one.
	 *                                  All module-specified player data is saved to the current preset, then all module-specified data is loaded from the specified preset.
	 *                                  The current preset will be updated to the specified preset.
	 */
	String switchCurrentPreset(ServerPlayerEntity player, String name) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param player the player this presets object belongs to
	 *               Saves all preset data for the current preset.
	 *               Helpful for exporting, displaying, etc.
	 */
	void saveCurrentPreset(ServerPlayerEntity player);

	/**
	 * @param player  the player this presets object belongs to
	 * @param id      a module identifier
	 * @param mutator a consumer that will modify the module while ducked
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 *                                  Allows you to "hot modify" the usually inaccessible current-preset data for the specified module.
	 *                                  Achieves this by saving the module, mutating it, then restoring it all in one swoop.
	 */
	void duckCurrentModule(ServerPlayerEntity player, Identifier id, Consumer<SwitchyModule> mutator) throws IllegalArgumentException, IllegalStateException;
}

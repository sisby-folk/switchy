package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.exception.ClassNotAssignableException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.exception.PresetNotFoundException;
import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles all Switchy interactions for a specific player, and holds all their data.
 * You were probably looking for this class.
 * A collection of {@link SwitchyPreset}.
 *
 * @author Sisby folk
 * @see SwitchyPresetsData
 * @see folk.sisby.switchy.api.SwitchyPlayer
 * @since 1.0.0
 */
public interface SwitchyPresets extends SwitchyPresetsData<SwitchyModule, SwitchyPreset> {
	/**
	 * Enables a module, creating empty instances in every preset.
	 *
	 * @param player the relevant player.
	 * @param id     a module identifier.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is enabled.
	 */
	void enableModule(ServerPlayerEntity player, Identifier id) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Disables a module, deleting its instances from every preset.
	 *
	 * @param player the relevant player.
	 * @param id     a module identifier.
	 * @param dryRun whether to skip disabling the module.
	 *               For use in situations where throwable-based validation is desired before confirming the action.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is disabled.
	 * @see SwitchyPresetsData#disableModule(Identifier)
	 */
	void disableModule(ServerPlayerEntity player, Identifier id, boolean dryRun) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Disables a module, deleting its instances from every preset.
	 *
	 * @param player the relevant player.
	 * @param id     a module identifier.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is disabled.
	 * @see SwitchyPresetsData#disableModule(Identifier)
	 */
	void disableModule(ServerPlayerEntity player, Identifier id) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Deletes a preset from this object.
	 *
	 * @param player the relevant player.
	 * @param name   the case-insensitive name of a preset.
	 * @param dryRun whether to skip deleting the preset.
	 *               For use in situations where throwable-based validation is desired before confirming the action.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws IllegalStateException   when the preset with the specified name is the current preset.
	 * @see SwitchyPresets#deletePreset(String)
	 */
	void deletePreset(ServerPlayerEntity player, String name, boolean dryRun) throws PresetNotFoundException, IllegalStateException;

	/**
	 * Deletes a preset from this object.
	 *
	 * @param player the relevant player.
	 * @param name   the case-insensitive name of a preset.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws IllegalStateException   when the preset with the specified name is the current preset.
	 * @see SwitchyPresets#deletePreset(String)
	 */
	void deletePreset(ServerPlayerEntity player, String name) throws PresetNotFoundException, IllegalStateException;

	/**
	 * Also hot-modifies the current preset when importing.
	 * Uses {@link SwitchyPresets#mutateModule(ServerPlayerEntity, String, Identifier, Consumer, Class)}.
	 *
	 * @param player the player this presets object belongs to.
	 * @param other  a collection of presets to import.
	 * @see SwitchyPresetsData#importFromOther(Map)
	 */
	void importFromOther(ServerPlayerEntity player, Map<String, SwitchyPreset> other);

	/**
	 * @param player the player this presets object belongs to.
	 * @param other  another presets object to import from.
	 * @see SwitchyPresets#importFromOther(ServerPlayerEntity, Map)
	 */
	void importFromOther(ServerPlayerEntity player, SwitchyPresets other);

	/**
	 * "Switches" from the current preset to the specified one.
	 * All module-specified player data is saved to the current preset, then all module-specified data is loaded from the specified preset.
	 * The current preset will be updated to the specified preset.
	 *
	 * @param player the player this presets object belongs to.
	 * @param name   the case-insensitive name of a preset.
	 * @return the (case-corrected) name of the new current preset.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws IllegalStateException   when the preset with the specified name is the current preset.
	 */
	String switchCurrentPreset(ServerPlayerEntity player, String name) throws PresetNotFoundException, IllegalStateException;

	/**
	 * Saves all preset data for the current preset.
	 * Helpful for exporting, displaying, etc.
	 *
	 * @param player the player this presets object belongs to.
	 */
	void saveCurrentPreset(ServerPlayerEntity player);

	/**
	 * Modify any module - even the current one.
	 * Achieves this by saving the module, mutating it, then restoring it all in one swoop.
	 *
	 * @param <ModuleType> the class of module to return.
	 * @param player       the player this presets object belongs to.
	 * @param name         the case-insensitive name of a preset.
	 * @param id           a module identifier.
	 * @param mutator      a consumer that will modify the module.
	 * @param clazz        the class of the specified module.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws ClassNotAssignableException when the specified module is not of {@code <ModuleType>}.
	 * @throws IllegalStateException   when the specified module is disabled.
	 */
	<ModuleType extends SwitchyModule> void mutateModule(ServerPlayerEntity player, String name, Identifier id, Consumer<ModuleType> mutator, Class<ModuleType> clazz) throws PresetNotFoundException, ModuleNotFoundException, ClassNotAssignableException, IllegalStateException;

	/**
	 * Modify a specific module across all presets.
	 *
	 * @param <ModuleType> the class of module to return.
	 * @param player       the player this presets object belongs to.
	 * @param id           a module identifier.
	 * @param mutator      a consumer that will modify the specified preset's module.
	 * @param clazz        the class of the specified module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws ClassNotAssignableException	when the specified module is not of {@code <ModuleType>}.
	 * @throws IllegalStateException   when the specified module is disabled.
	 */
	<ModuleType extends SwitchyModule> void mutateAllOfModule(ServerPlayerEntity player, Identifier id, BiConsumer<String, ModuleType> mutator, Class<ModuleType> clazz) throws ModuleNotFoundException, ClassNotAssignableException, IllegalStateException;

	/**
	 * Modify all modules in a preset.
	 *
	 * @param player  the player this presets object belongs to.
	 * @param name    the case-insensitive name of a preset.
	 * @param mutator a consumer that will modify the specified preset's module.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 */
	void mutatePresetOfModules(ServerPlayerEntity player, String name, BiConsumer<Identifier, SwitchyModule> mutator) throws PresetNotFoundException;
}

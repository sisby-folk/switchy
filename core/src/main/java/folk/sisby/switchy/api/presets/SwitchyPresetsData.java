package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

/**
 * @author Sisby folk
 * @since 2.0.0
 * A collection of {@link SwitchyPreset}, holding data, and a reference to their {@link SwitchySerializable} modules.
 * All contained presets have identical modules (called "enabled" modules).
 */
public interface SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> extends SwitchySerializable {
	/**
	 * NBT key for the current preset
	 */
	String KEY_PRESET_CURRENT = "current";
	/**
	 * NBT key for the list of enabled modules
	 */
	String KEY_PRESET_MODULE_ENABLED = "enabled";
	/**
	 * NBT key for the list of disabled modules
	 */
	String KEY_PRESET_MODULE_DISABLED = "disabled";
	/**
	 * NBT key for the list of presets
	 */
	String KEY_PRESET_LIST = "list";

	/**
	 * @param other a map of presets to import into this object
	 *              Imports a set of presets, merging by replacing modules where preset names collide.
	 *              Only registered, enabled modules will be imported.
	 */
	void importFromOther(Map<String, Preset> other);

	/**
	 * @param preset a named preset object
	 * @throws IllegalStateException when a preset with the provided name already exists
	 */
	void addPreset(Preset preset) throws IllegalStateException;

	/**
	 * @param name the desired name for the new preset.
	 * @return the newly created preset
	 * @throws IllegalStateException when a preset with the provided name already exists
	 */
	Preset newPreset(String name) throws IllegalStateException;

	/**
	 * @param name   the case-insensitive name of a preset
	 * @param dryRun whether to skip deleting the preset
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when the preset with the specified name is the current preset
	 * @see SwitchyPresets#deletePreset(String)
	 * For use in situations where throwable-based validation is desired before confirming the action.
	 */
	@ApiStatus.Internal
	void deletePreset(String name, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param name the case-insensitive name of a preset
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when the preset with the specified name is the current preset
	 *                                  Deletes the specified preset, along with any associated data.
	 */
	void deletePreset(String name) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param name    the case-insensitive name of a preset
	 * @param newName the new name for the specified preset. a single word matching {@code azAZ09_-.+}
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 * @throws IllegalStateException    when a preset with the provided name already exists
	 *                                  Safely changes the name of the specified preset.
	 */
	void renamePreset(String name, String newName) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param id     a module identifier
	 * @param dryRun whether to skip disabling the module
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 * @see SwitchyPresets#disableModule(Identifier)
	 * For use in situations where throwable-based validation is desired before confirming the action.
	 */
	@ApiStatus.Internal
	void disableModule(Identifier id, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param id a module identifier
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 *                                  Disables a module, deleting all instances of it across presets
	 */
	void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param id a module identifier
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is enabled
	 *                                  Enables a module, creating empty instances in every preset
	 */
	void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @return the current preset
	 */
	@ApiStatus.Internal
	Preset getCurrentPreset();

	/**
	 * @param name the case-insensitive name of a preset
	 * @throws IllegalArgumentException when a preset with the specified name doesn't exist
	 */
	@ApiStatus.Internal
	void setCurrentPreset(String name) throws IllegalArgumentException;

	/**
	 * @return the name of the current preset
	 */
	String getCurrentPresetName();

	/**
	 * @return all contained presets
	 */
	@ApiStatus.Internal
	Map<String, Preset> getPresets();

	/**
	 * @param name the case-insensitive name of a preset
	 * @return the specified preset
	 */
	@ApiStatus.Internal
	Preset getPreset(String name);

	/**
	 * @return a list of each preset name
	 */
	List<String> getPresetNames();

	/**
	 * @param name the case-insensitive name of a preset
	 * @return whether that preset exists
	 */
	boolean containsPreset(String name);

	/**
	 * @return the enabled status of all modules
	 */
	@ApiStatus.Internal
	Map<Identifier, Boolean> getModules();

	/**
	 * @param id a module identifier
	 * @return a map of each preset to the specified module
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 * @throws IllegalStateException    when the specified module is disabled
	 */
	Map<String, Module> getAllOfModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @return a list of all enabled module IDs
	 */
	List<Identifier> getEnabledModules();

	/**
	 * @return a list of all disabled module IDs
	 */
	List<Identifier> getDisabledModules();

	/**
	 * @param id a module identifier
	 * @return whether a module is registered at all in this object
	 */
	boolean containsModule(Identifier id);

	/**
	 * @param id a module identifier
	 * @return whether the specified module is enabled
	 * @throws IllegalArgumentException when the specified module doesn't exist
	 */
	boolean isModuleEnabled(Identifier id) throws IllegalArgumentException;

	/**
	 * @return a list of all enabled module names (paths)
	 */
	List<String> getEnabledModuleNames();

	/**
	 * @return a text representation of all module IDs, showing paths with full IDs on hover
	 */
	MutableText getEnabledModuleText();

	/**
	 * @return the names of every preset as a formatted list
	 */
	@Override
	String toString();
}

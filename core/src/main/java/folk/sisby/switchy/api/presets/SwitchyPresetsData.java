package folk.sisby.switchy.api.presets;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.exception.ClassNotAssignableException;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.exception.PresetNotFoundException;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

/**
 * A collection of {@link SwitchyPreset}, holding data, and a reference to their {@link SwitchySerializable} modules.
 * All contained presets have identical modules (called "enabled" modules).
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public interface SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> extends SwitchySerializable {
	/**
	 * NBT key for the current preset.
	 */
	String KEY_PRESET_CURRENT = "current";
	/**
	 * NBT key for the list of enabled modules.
	 */
	String KEY_PRESET_MODULE_ENABLED = "enabled";
	/**
	 * NBT key for the list of disabled modules.
	 */
	String KEY_PRESET_MODULE_DISABLED = "disabled";
	/**
	 * NBT key for the compound of presets.
	 */
	String KEY_PRESETS = "list";
	/**
	 * NBT key for compound of module configs.
	 */
	String KEY_MODULE_CONFIGS = "moduleConfigs";

	/**
	 * Adds a preset to this object.
	 *
	 * @param preset a named preset object.
	 * @throws IllegalStateException when a preset with the provided name already exists.
	 */
	void addPreset(Preset preset) throws IllegalStateException;

	/**
	 * Creates a new preset and adds it to this object.
	 *
	 * @param name the desired name for the new preset.
	 * @return the newly created preset.
	 * @throws InvalidWordException  when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 * @throws IllegalStateException when a preset with the provided name already exists.
	 */
	Preset newPreset(String name) throws InvalidWordException, IllegalStateException;

	/**
	 * Deletes a preset from this object.
	 * In a presets object, a module must be disabled using {@link SwitchyPresets#deletePreset(ServerPlayerEntity, String, boolean)}.
	 *
	 * @param name   the case-insensitive name of a preset.
	 * @param dryRun whether to skip deleting the preset.
	 *               For use in situations where throwable-based validation is desired before confirming the action.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws IllegalStateException   when the preset with the specified name is the current preset.
	 * @see SwitchyPresets#deletePreset(String)
	 */
	void deletePreset(String name, boolean dryRun) throws PresetNotFoundException, IllegalStateException;

	/**
	 * Deletes a preset from this object.
	 * In a presets object, a module must be disabled using {@link SwitchyPresets#deletePreset(ServerPlayerEntity, String)}.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws IllegalStateException   when the preset with the specified name is the current preset.
	 *                                 Deletes the specified preset, along with any associated data.
	 */
	void deletePreset(String name) throws PresetNotFoundException, IllegalStateException;

	/**
	 * Safely changes the name of the specified preset.
	 *
	 * @param name    the case-insensitive name of a preset.
	 * @param newName the new name for the specified preset. a single word matching {@code azAZ09_-.+}.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws InvalidWordException    when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 * @throws IllegalStateException   when a preset with the provided name already exists.
	 */
	void renamePreset(String name, String newName) throws PresetNotFoundException, InvalidWordException, IllegalStateException;

	/**
	 * Disables a module, deleting its instances from every preset.
	 * In a presets object, a module must be disabled using {@link SwitchyPresets#disableModule(ServerPlayerEntity, Identifier, boolean)}.
	 *
	 * @param id     a module identifier.
	 * @param dryRun whether to skip disabling the module.
	 *               For use in situations where throwable-based validation is desired before confirming the action.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is disabled.
	 * @see SwitchyPresets#disableModule(Identifier)
	 */
	void disableModule(Identifier id, boolean dryRun) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Disables a module, deleting its instances from every preset.
	 * In a presets object, a module must be disabled using {@link SwitchyPresets#disableModule(ServerPlayerEntity, Identifier)}.
	 *
	 * @param id a module identifier.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is disabled.
	 */
	void disableModule(Identifier id) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Enables a module, creating empty instances in every preset.
	 *
	 * @param id a module identifier.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is enabled.
	 */
	void enableModule(Identifier id) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Gets the current preset.
	 *
	 * @return the current preset.
	 */
	Preset getCurrentPreset();

	/**
	 * Unsafely sets the current preset reference without switching any data.
	 * Causes data loss when the object is in use.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 */
	@ApiStatus.Internal
	void setCurrentPreset(String name) throws PresetNotFoundException;

	/**
	 * Gets the name of the current preset.
	 *
	 * @return the name of the current preset.
	 */
	String getCurrentPresetName();

	/**
	 * Gets all contained presets.
	 *
	 * @return all contained presets.
	 */
	Map<String, Preset> getPresets();

	/**
	 * Gets the specified preset.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @return the specified preset.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 */
	Preset getPreset(String name) throws PresetNotFoundException;

	/**
	 * Gets a list of all preset names.
	 *
	 * @return a list of each preset name.
	 */
	List<String> getPresetNames();

	/**
	 * Whether a preset with the specified name exists.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @return true if the preset exists, false otherwise.
	 */
	boolean containsPreset(String name);

	/**
	 * Gets a map representing the enabled status of all modules.
	 *
	 * @return the enabled status of all modules.
	 */
	Map<Identifier, Boolean> getModules();

	/**
	 * Gets the specified module.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @param id   a module identifier.
	 * @return the specified module stored in the specified preset.
	 * @throws PresetNotFoundException when a preset with the specified name doesn't exist.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is disabled.
	 */
	Module getModule(String name, Identifier id) throws PresetNotFoundException, ModuleNotFoundException, IllegalStateException;

	/**
	 * Gets the specified module.
	 *
	 * @param <ModuleType> the class of module to return.
	 * @param name         the case-insensitive name of a preset.
	 * @param id           a module identifier.
	 * @param clazz        the class of the specified module.
	 * @return the specified module stored in the specified preset.
	 * @throws PresetNotFoundException     when a preset with the specified name doesn't exist.
	 * @throws ModuleNotFoundException     when a module with the specified ID doesn't exist.
	 * @throws ClassNotAssignableException when the specified module is not of {@code <ModuleType>}.
	 * @throws IllegalStateException       when the specified module is disabled.
	 */
	<ModuleType extends Module> ModuleType getModule(String name, Identifier id, Class<ModuleType> clazz) throws PresetNotFoundException, ModuleNotFoundException, ClassNotAssignableException, IllegalStateException;

	/**
	 * Gets every instance of a module mapped by preset name.
	 *
	 * @param id a module identifier.
	 * @return a map of each preset to the specified module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @throws IllegalStateException   when the specified module is disabled.
	 */
	Map<String, Module> getAllOfModule(Identifier id) throws ModuleNotFoundException, IllegalStateException;

	/**
	 * Gets every instance of a module mapped by preset name.
	 *
	 * @param <ModuleType> the class of module to return.
	 * @param id           a module identifier.
	 * @param clazz        the class of the specified module.
	 * @return a map of each preset to the specified module.
	 * @throws ModuleNotFoundException     when a module with the specified ID doesn't exist.
	 * @throws ClassNotAssignableException when the specified module is not of {@code <ModuleType>}.
	 * @throws IllegalStateException       when the specified module is disabled.
	 */
	<ModuleType extends Module> Map<String, ModuleType> getAllOfModule(Identifier id, Class<ModuleType> clazz) throws ModuleNotFoundException, ClassNotAssignableException, IllegalStateException;

	/**
	 * Gets a list of all enabled modules as IDs.
	 *
	 * @return a list of all enabled module IDs.
	 */
	List<Identifier> getEnabledModules();

	/**
	 * * Gets a list of all disabled modules as IDs.
	 *
	 * @return a list of all disabled module IDs.
	 */
	List<Identifier> getDisabledModules();

	/**
	 * Whether a module is registered in this object.
	 * Registered modules can be enabled and disabled.
	 *
	 * @param id a module identifier.
	 * @return true if a module is registered, false otherwise.
	 */
	boolean containsModule(Identifier id);

	/**
	 * Whether a specified module is enabled.
	 *
	 * @param id a module identifier.
	 * @return true if the module is enabled, false otherwise.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	boolean isModuleEnabled(Identifier id) throws ModuleNotFoundException;

	/**
	 * Gets the player-level configuration object for a module, if one exists.
	 *
	 * @param <ConfigType> the type of config to return.
	 * @param id           a module identifier.
	 * @param clazz        the class of the specified config.
	 * @return the module configuration object for this module, or null if it doesn't exist
	 * @throws ModuleNotFoundException     when a module with the specified ID doesn't exist.
	 * @throws ClassNotAssignableException when the specified module's config is not of {@code <ConfigType>}.
	 * @throws IllegalStateException       when the specified module is disabled.
	 */
	<ConfigType extends SwitchySerializable> ConfigType getModuleConfig(Identifier id, Class<ConfigType> clazz) throws ModuleNotFoundException, ClassNotAssignableException, IllegalStateException;

	/**
	 * Gets a list of all enabled modules as ID paths.
	 *
	 * @return a list of all enabled module names (paths).
	 */
	List<String> getEnabledModuleNames();

	/**
	 * Creates a compact text representation of all modules.
	 *
	 * @return a text representation of all module IDs, showing paths with full IDs on hover.
	 */
	MutableText getEnabledModuleText();

	/**
	 * @return the names of every preset as a formatted list.
	 */
	@Override
	String toString();
}

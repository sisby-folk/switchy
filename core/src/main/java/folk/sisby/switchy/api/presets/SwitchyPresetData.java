package folk.sisby.switchy.api.presets;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.exception.ClassNotAssignableException;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * A named, NBT-Serializable object made of {@link SwitchySerializable} modules.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public interface SwitchyPresetData<Module extends SwitchySerializable> extends SwitchySerializable {
	/**
	 * Gets a map of all modules in this preset by ID.
	 *
	 * @return all modules in this preset, mapped by their IDs.
	 */
	@ApiStatus.Internal
	Map<Identifier, Module> getModules();

	/**
	 * Gets a map of all modules that are instances of the specified class.
	 *
	 * @param <ModuleType> the class of module to return.
	 * @param clazz        the class of the desired modules.
	 * @return a map of all clazz modules.
	 */
	<ModuleType> Map<Identifier, ModuleType> getModules(Class<ModuleType> clazz);

	/**
	 * Gets the specified module.
	 *
	 * @param id a module identifier.
	 * @return the specified module stored in this preset.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	Module getModule(Identifier id);

	/**
	 * Gets the specified module.
	 *
	 * @param <ModuleType> the class of module to return.
	 * @param id           a module identifier.
	 * @param clazz        the class of the specified module.
	 * @return the specified module stored in this preset.
	 * @throws ModuleNotFoundException     when a module with the specified ID doesn't exist.
	 * @throws ClassNotAssignableException when the specified module is not of {@code <ModuleType>}.
	 */
	<ModuleType extends Module> ModuleType getModule(Identifier id, Class<ModuleType> clazz) throws ModuleNotFoundException, ClassNotAssignableException;

	/**
	 * Adds or replaces the specified module.
	 *
	 * @param id     a module identifier.
	 * @param module a module to add to or replace in the module map.
	 */
	void putModule(Identifier id, Module module);

	/**
	 * Whether a module with the specified ID exists in this preset.
	 *
	 * @param id a module identifier.
	 * @return true if the preset contains this module, false otherwise.
	 */
	boolean containsModule(Identifier id);

	/**
	 * Removes the specified module from the preset.
	 *
	 * @param id a module identifier.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	void removeModule(Identifier id) throws ModuleNotFoundException;

	/**
	 * @return the name of this preset.
	 */
	String getName();

	/**
	 * Sets this object's internal preset name.
	 * Must be a single word matching {@code azAZ09_-.+}.
	 * In a presets object, a preset must be renamed using {@link SwitchyPresets#renamePreset(String, String)}.
	 *
	 * @param name a new name for this preset.
	 * @throws InvalidWordException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 */
	@ApiStatus.Internal
	void setName(String name) throws InvalidWordException;
}

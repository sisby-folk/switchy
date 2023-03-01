package folk.sisby.switchy.api.presets;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.SwitchySerializable;
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
	 * @return All modules in this preset, mapped by their IDs.
	 */
	@ApiStatus.Internal
	Map<Identifier, Module> getModules();

	/**
	 * Gets the specified module.
	 *
	 * @param id a module identifier.
	 * @return the specified module stored in this preset.
	 */
	@ApiStatus.Internal
	Module getModule(Identifier id);

	/**
	 * Adds or replaces the specified module.
	 *
	 * @param id     a module identifier.
	 * @param module a module to add to or replace in the module map.
	 */
	@ApiStatus.Internal
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
	 */
	void removeModule(Identifier id);

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
	 * @throws IllegalArgumentException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)})
	 */
	@ApiStatus.Internal
	void setName(String name) throws IllegalArgumentException;
}

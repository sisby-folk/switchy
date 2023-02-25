package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * @author Sisby folk
 * @since 2.0.0
 * A named, NBT-Serializable object made of {@link SwitchySerializable} modules.
 */
public interface SwitchyPresetData<Module extends SwitchySerializable> extends SwitchySerializable {
	/**
	 * @return All modules in this preset, mapped by their IDs
	 */
	@ApiStatus.Internal
	Map<Identifier, Module> getModules();

	/**
	 * @param id a module identifier
	 * @return the specified module stored in this preset
	 */
	@ApiStatus.Internal
	Module getModule(Identifier id);

	/**
	 * @param id a module identifier
	 * @param module a module to add to or replace in the module map
	 */
	@ApiStatus.Internal
	void putModule(Identifier id, Module module);

	/**
	 * @param id a module identifier
	 * @return whether the preset contains a module with the specified ID
	 */
	boolean containsModule(Identifier id);

	/**
	 * @param id a module identifier
	 * removes the specified module from the preset
	 */
	void removeModule(Identifier id);

	/**
	 * @return the name of this preset
	 */
	String getName();

	/**
	 * @param name a new name for this preset
	 * Must be a single word matching {@code azAZ09_-.+}
	 * In a presets object, a preset must be renamed using {@link SwitchyPresets#renamePreset(String, String)}
	 */
	@ApiStatus.Internal
	void setName(String name);
}

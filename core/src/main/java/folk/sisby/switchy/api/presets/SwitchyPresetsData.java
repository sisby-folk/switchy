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
 * A collection of {@link SwitchyPreset}, holding data, a reference for their {@link SwitchySerializable} modules
 */
public interface SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> extends SwitchySerializable {
	String KEY_PRESET_CURRENT = "current";
	String KEY_PRESET_MODULE_ENABLED = "enabled";
	String KEY_PRESET_MODULE_DISABLED = "disabled";
	String KEY_PRESET_LIST = "list";

	/**
	 * @param other
	 */
	void importFromOther(Map<String, Preset> other);

	/**
	 * @param name
	 * @throws IllegalArgumentException
	 */
	void setCurrentPreset(String name) throws IllegalArgumentException;

	/**
	 * @param preset
	 * @throws IllegalStateException
	 */
	void addPreset(Preset preset) throws IllegalStateException;

	/**
	 * @param name
	 * @return
	 * @throws IllegalStateException
	 */
	Preset newPreset(String name) throws IllegalStateException;

	/**
	 * @param name
	 * @param dryRun
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	@ApiStatus.Internal
	void deletePreset(String name, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param name
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	void deletePreset(String name) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param name the case-insensitive name of a preset
	 * @param newName the new name for the specified preset. a single word matching {@code azAZ09_-.+}
	 * @throws IllegalArgumentException when a preset with that name doesn't exist
	 * @throws IllegalStateException when a preset with the desired new name already exists
	 */
	void renamePreset(String name, String newName) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param id
	 * @param dryRun
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	@ApiStatus.Internal
	void disableModule(Identifier id, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param id
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @param id
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @return
	 */
	@ApiStatus.Internal
	Preset getCurrentPreset();

	/**
	 * @return
	 */
	String getCurrentPresetName();

	/**
	 * @return
	 */
	@ApiStatus.Internal
	Map<String, Preset> getPresets();

	/**
	 * @param name
	 * @return
	 */
	@ApiStatus.Internal
	Preset getPreset(String name);

	/**
	 * @return
	 */
	List<String> getPresetNames();

	/**
	 * @param name
	 * @return
	 */
	boolean containsPreset(String name);

	/**
	 * @return
	 */
	@ApiStatus.Internal
	Map<Identifier, Boolean> getModules();

	/**
	 * @param id
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	Map<String, Module> getAllOfModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	/**
	 * @return
	 */
	List<Identifier> getEnabledModules();

	/**
	 * @return
	 */
	List<Identifier> getDisabledModules();

	/**
	 * @param id
	 * @return
	 */
	boolean containsModule(Identifier id);

	/**
	 * @param id
	 * @return
	 * @throws IllegalArgumentException
	 */
	boolean isModuleEnabled(Identifier id) throws IllegalArgumentException;

	/**
	 * @return
	 */
	List<String> getEnabledModuleNames();

	/**
	 * @return
	 */
	MutableText getEnabledModuleText();

	/**
	 * @return the names of every preset as a formatted list
	 */
	@Override
	String toString();
}

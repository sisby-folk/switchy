package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

public interface SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> extends SwitchySerializable {
	String KEY_PRESET_CURRENT = "current";
	String KEY_PRESET_MODULE_ENABLED = "enabled";
	String KEY_PRESET_MODULE_DISABLED = "disabled";
	String KEY_PRESET_LIST = "list";

	void fillFromNbt(NbtCompound nbt);

	NbtCompound toNbt();

	void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent);

	void importFromOther(Map<String, Preset> other);

	void setCurrentPreset(String name) throws IllegalArgumentException;

	void addPreset(Preset preset) throws IllegalStateException;

	Preset newPreset(String name) throws IllegalStateException;

	void deletePreset(String name, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	void deletePreset(String name) throws IllegalArgumentException, IllegalStateException;

	void renamePreset(String oldName, String newName) throws IllegalArgumentException, IllegalStateException;

	void disableModule(Identifier id, boolean dryRun) throws IllegalArgumentException, IllegalStateException;

	void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	@ApiStatus.Internal
	Preset getCurrentPreset();

	String getCurrentPresetName();

	@ApiStatus.Internal
	Map<String, Preset> getPresets();

	@ApiStatus.Internal
	Preset getPreset(String name);

	List<String> getPresetNames();

	boolean containsPreset(String name);

	@ApiStatus.Internal
	Map<Identifier, Boolean> getModules();

	Map<String, Module> getAllOfModule(Identifier id) throws IllegalArgumentException, IllegalStateException;

	List<Identifier> getEnabledModules();

	List<Identifier> getDisabledModules();

	boolean containsModule(Identifier id);

	boolean isModuleEnabled(Identifier id) throws IllegalArgumentException;

	List<String> getEnabledModuleNames();

	MutableText getEnabledModuleText();

	@Override
	String toString();
}

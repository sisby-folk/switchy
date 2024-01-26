package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.exception.ClassNotAssignableException;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.exception.PresetNotFoundException;
import folk.sisby.switchy.api.presets.SwitchyPresetData;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static folk.sisby.switchy.util.Feedback.getIdListText;

/**
 * @author Sisby folk
 * @see SwitchyPresetsData
 * @since 2.0.0
 */
public abstract class SwitchyPresetsDataImpl<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> implements SwitchyPresetsData<Module, Preset> {
	private final Map<String, Preset> presets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<Identifier, SwitchySerializable> moduleConfigs = new HashMap<>();
	private final Map<Identifier, Boolean> modules;
	private final Map<Identifier, Boolean> backup = new HashMap<>();
	private final boolean forPlayer;
	private final Logger logger;
	private Preset currentPreset;

	/**
	 * Constructs an instance of the object.
	 *
	 * @param modules           the enabled status of modules.
	 * @param forPlayer         whether the presets object is "for a player" - affects recovering lost presets, and logging failures.
	 * @param logger            the logger to use for construction failures.
	 */
	SwitchyPresetsDataImpl(Map<Identifier, Boolean> modules, boolean forPlayer, Logger logger) {
		this.modules = modules;
		this.forPlayer = forPlayer;
		this.logger = logger;
	}

	/**
	 * Constructs a preset instance.
	 *
	 * @param name    the desired name for the new preset.
	 * @param modules a map representing which modules are enabled.
	 * @return a new preset.
	 */
	public abstract Preset constructPreset(String name, Map<Identifier, Boolean> modules);

	/**
	 * Supplies a module instance.
	 *
	 * @param id     a module identifier.
	 * @return an instance of the module.
	 */
	public abstract Module supplyModule(Identifier id);

	/**
	 * Supplies the player-level module configuration for the given module.
	 *
	 * @param id     a module identifier.
	 * @return the configuration object for the module, or null if it doesn't exist.
	 */
	public abstract @Nullable SwitchySerializable supplyModuleConfig(Identifier id);

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE), true, !forPlayer);
		toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE), false, !forPlayer);
		if (!forPlayer) { // Disable non-enabled modules by default for non-player presets.
			List<Identifier> enabledModules = nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).toList();
			modules.forEach((id, enabled) -> modules.put(id, enabledModules.contains(id)));
		}

		NbtCompound configCompound = nbt.getCompound(KEY_MODULE_CONFIGS);
		for (String key : configCompound.getKeys()) {
			try {
				SwitchySerializable config = setConfig(new Identifier(key));
				if (config != null) {
					config.fillFromNbt(configCompound.getCompound(key));
				}
			} catch (InvalidIdentifierException ignoredInvalidIdentifier) {
				logger.warn("[Switchy] Player data contained invalid module config '{}'. Data may have been lost.", key);
			} catch (ModuleNotFoundException ignoredModuleNotFound) {
				logger.warn("[Switchy] Player data contained missing module config '{}'. Data may have been lost.", key);
			} catch (IllegalStateException ignored) {
				logger.warn("[Switchy] Player data contained disabled module config '{}'. Data may have been lost.", key);
			}
		}
		getEnabledModules().stream().filter(key -> !moduleConfigs.containsKey(key)).forEach(this::setConfig);

		NbtCompound presetsCompound = nbt.getCompound(KEY_PRESETS);
		for (String key : presetsCompound.getKeys()) {
			try {
				newPreset(key).fillFromNbt(presetsCompound.getCompound(key));
			} catch (IllegalStateException ignoredPresetExists) {
				logger.warn("[Switchy] Player data contained duplicate preset '{}'. Data may have been lost.", key);
			} catch (InvalidWordException ignored) {
				logger.warn("[Switchy] Player data contained invalid preset '{}'. Data may have been lost.", key);
			}
		}

		if (forPlayer) {
			if (nbt.contains(KEY_PRESET_CURRENT)) try {
				setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT));
			} catch (PresetNotFoundException ignored) {
				logger.warn("[Switchy] Unable to set current preset from data. Data may have been lost.");
			}

			if (presets.isEmpty() || getCurrentPreset() == null) {
				// Recover current data as "Default" preset
				setCurrentPreset(newPreset("default").getName());
			}
		}
	}

	private @Nullable SwitchySerializable setConfig(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified preset already exists.");
		SwitchySerializable config = supplyModuleConfig(id);
		return config != null ? moduleConfigs.put(id, supplyModuleConfig(id)) : null;
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		modules.forEach((id, enabled) -> {
			if (enabled) enabledList.add(NbtString.of(id.toString()));
			if (!enabled) disabledList.add(NbtString.of(id.toString()));
		});

		NbtCompound configCompound = new NbtCompound();
		moduleConfigs.forEach((id, config) -> configCompound.put(id.toString(), config.toNbt()));
		outNbt.put(KEY_MODULE_CONFIGS, configCompound);

		backup.forEach((id, enabled) -> {
			if (!modules.containsKey(id)) {
				if (enabled) enabledList.add(NbtString.of(id.toString()));
				if (!enabled) disabledList.add(NbtString.of(id.toString()));
			}
		});

		outNbt.put(KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound presetsCompound = new NbtCompound();
		for (Preset preset : presets.values()) {
			presetsCompound.put(preset.getName(), preset.toNbt());
		}
		outNbt.put(KEY_PRESETS, presetsCompound);

		if (getCurrentPreset() != null) { // non-player presets won't have one.
			outNbt.putString(KEY_PRESET_CURRENT, getCurrentPresetName());
		}
		return outNbt;
	}

	/**
	 * @param list    serialized module list.
	 * @param enabled whether to enable or disable modules from the list.
	 * @param silent  whether to log missing or invalid modules.
	 *                Toggles the enabled module map using an NBTList.
	 */
	void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		list.forEach((e) -> {
			Identifier id;
			if ((id = Identifier.tryParse(e.asString())) != null && modules.containsKey(id)) {
				modules.put(id, enabled);
			} else if (!silent) {
				logger.warn("[Switchy] Unable to toggle a module - Was a module unloaded?");
				logger.warn("[Switchy] Key: {}", e.asString());
				if (id != null) {
					logger.warn("[Switchy] Saved as lost puppy {} module.", enabled ? "enabled" : "disabled");
					backup.put(id, enabled);
				}
			}
		});
	}

	@Override
	public void addPreset(Preset preset) throws IllegalStateException {
		if (containsPreset(preset.getName())) throw new IllegalStateException("Specified preset already exists.");
		presets.put(preset.getName(), preset);
	}

	@Override
	public Preset newPreset(String name) throws InvalidWordException, IllegalStateException {
		if (presets.containsKey(name)) throw new IllegalStateException("Specified preset already exists.");
		Preset newPreset = constructPreset(name, modules);
		presets.put(name, newPreset);
		return newPreset;
	}

	@Override
	public void deletePreset(String name, boolean dryRun) throws PresetNotFoundException, IllegalStateException {
		if (!presets.containsKey(name)) throw new PresetNotFoundException();
		if (getCurrentPresetName().equalsIgnoreCase(name))
			throw new IllegalStateException("Specified preset is current");
		if (dryRun) return;
		presets.remove(name);
	}

	@Override
	public void deletePreset(String name) throws PresetNotFoundException, IllegalStateException {
		deletePreset(name, false);
	}

	@Override
	public void renamePreset(String name, String newName) throws PresetNotFoundException, IllegalStateException {
		if (!presets.containsKey(name)) throw new PresetNotFoundException();
		if (presets.containsKey(newName)) throw new IllegalStateException("Specified preset name already exists");
		Preset preset = presets.remove(name);
		preset.setName(newName);
		presets.put(newName, preset);
	}

	@Override
	public void disableModule(Identifier id, boolean dryRun) throws ModuleNotFoundException, IllegalStateException {
		if (!modules.containsKey(id)) throw new ModuleNotFoundException();
		if (!modules.get(id)) throw new IllegalStateException("Specified module is disabled");
		if (dryRun) return;
		modules.put(id, false);
		moduleConfigs.remove(id);
		presets.forEach((name, preset) -> preset.removeModule(id));
	}

	@Override
	public void disableModule(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		disableModule(id, false);
	}

	/**
	 * Internal implementation for {@link SwitchyPresetsDataImpl#enableModule(Identifier)}.
	 *
	 * @param id a module identifier.
	 * @return the list of module instances that were enabled.
	 */
	protected List<Module> enableModuleAndReturn(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		if (!modules.containsKey(id)) throw new ModuleNotFoundException();
		if (modules.get(id)) throw new IllegalStateException("Specified module is enabled");
		List<Module> outList = new ArrayList<>();
		modules.put(id, true);
		SwitchySerializable config = supplyModuleConfig(id);
		if (config != null) {
			moduleConfigs.put(id, config);
		}
		presets.values().forEach(preset -> {
			Module module = supplyModule(id);
			preset.putModule(id, module);
			outList.add(module);
		});
		return outList;
	}

	@Override
	public void enableModule(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		enableModuleAndReturn(id);
	}

	@Override
	@ApiStatus.Internal
	public Preset getCurrentPreset() {
		return currentPreset;
	}

	@Override
	public void setCurrentPreset(String name) throws PresetNotFoundException {
		if (!presets.containsKey(name)) throw new PresetNotFoundException();
		currentPreset = presets.get(name);
	}

	@Override
	public String getCurrentPresetName() {
		return currentPreset.getName();
	}

	@Override
	@ApiStatus.Internal
	public Map<String, Preset> getPresets() {
		return presets;
	}

	@Override
	@ApiStatus.Internal
	public Preset getPreset(String name) throws PresetNotFoundException {
		if (!presets.containsKey(name)) throw new PresetNotFoundException();
		return presets.get(name);
	}

	@Override
	public List<String> getPresetNames() {
		return presets.keySet().stream().sorted().toList();
	}

	@Override
	public boolean containsPreset(String name) {
		return presets.containsKey(name);
	}

	@Override
	@ApiStatus.Internal
	public Map<Identifier, Boolean> getModules() {
		return modules;
	}

	@Override
	public Module getModule(String name, Identifier id) throws PresetNotFoundException, ModuleNotFoundException, IllegalStateException {
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified module is disabled");
		return getPreset(name).getModule(id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ModuleType extends Module> ModuleType getModule(String name, Identifier id, Class<ModuleType> clazz) throws PresetNotFoundException, ModuleNotFoundException, ClassNotAssignableException, IllegalStateException {
		Module module = getModule(name, id);
		if (!clazz.isAssignableFrom(module.getClass()))
			throw new ClassNotAssignableException("Module '" + id.toString(), module, clazz);
		return (ModuleType) module;
	}

	@Override
	public Map<String, Module> getAllOfModule(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified module is disabled");
		Map<String, Module> outMap = new HashMap<>();
		presets.forEach((name, preset) -> outMap.put(name, preset.getModule(id)));
		return outMap;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ModuleType extends Module> Map<String, ModuleType> getAllOfModule(Identifier id, Class<ModuleType> clazz) throws ModuleNotFoundException, ClassNotAssignableException, IllegalStateException {
		Map<String, Module> modules = getAllOfModule(id);
		Map<String, ModuleType> outModules = new HashMap<>();
		modules.forEach((name, module) -> {
			if (!clazz.isAssignableFrom(module.getClass()))
				throw new ClassNotAssignableException("Module '" + id.toString(), module, clazz);
			outModules.put(name, (ModuleType) module);
		});
		return outModules;
	}

	@Override
	public List<Identifier> getEnabledModules() {
		return modules.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
	}

	@Override
	public List<Identifier> getDisabledModules() {
		return modules.entrySet().stream().filter((e) -> !e.getValue()).map(Map.Entry::getKey).toList();
	}

	@Override
	public boolean containsModule(Identifier id) {
		return modules.containsKey(id);
	}

	@Override
	public boolean isModuleEnabled(Identifier id) throws ModuleNotFoundException {
		if (!containsModule(id)) throw new ModuleNotFoundException();
		return modules.get(id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ConfigType extends SwitchySerializable> ConfigType getModuleConfig(Identifier id, Class<ConfigType> clazz) throws ModuleNotFoundException, ClassNotAssignableException, IllegalStateException {
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified module is disabled");
		SwitchySerializable config = moduleConfigs.get(id);
		if (config == null) return null;
		if (!clazz.isAssignableFrom(config.getClass())) throw new ClassNotAssignableException("Module Config '" + id.toString(), config, clazz);
		return (ConfigType) config;
	}

	@Override
	public List<String> getEnabledModuleNames() {
		return getEnabledModules().stream().map(Identifier::getPath).toList();
	}

	@Override
	public MutableText getEnabledModuleText() {
		return getIdListText(getEnabledModules());
	}

	@Override
	public String toString() {
		return presets.keySet().toString();
	}
}

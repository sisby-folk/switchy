package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static folk.sisby.switchy.util.Feedback.getIdListText;

public class SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> implements SwitchySerializable {
	private final Map<String, Preset> presets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<Identifier, Boolean> modules;
	private Preset currentPreset;

	private final BiFunction<String, Map<Identifier, Boolean>, Preset> presetConstructor;
	private final Function<Identifier, Module> moduleSupplier;
	private final boolean forPlayer;
	private final Logger logger;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ENABLED = "enabled";
	public static final String KEY_PRESET_MODULE_DISABLED = "disabled";
	public static final String KEY_PRESET_LIST = "list";

	SwitchyPresetsData(Map<Identifier, Boolean> modules, BiFunction<String, Map<Identifier, Boolean>, Preset> presetConstructor, Function<Identifier, Module> moduleSupplier, boolean forPlayer, Logger logger) {
		this.modules = modules;
		this.presetConstructor = presetConstructor;
		this.moduleSupplier = moduleSupplier;
		this.forPlayer = forPlayer;
		this.logger = logger;
	}

	public void fillFromNbt(NbtCompound nbt) {
		toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE), true, !forPlayer);
		toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE), false, !forPlayer);

		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			try {
				newPreset(key).fillFromNbt(listNbt.getCompound(key));
			} catch (IllegalStateException ignored) {
				logger.warn("[Switchy] Player data contained duplicate preset '{}'. Data may have been lost.", key);
			}
		}

		if (forPlayer) {
			if (nbt.contains(KEY_PRESET_CURRENT))
				try {
					setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT));
				} catch (IllegalArgumentException ignored) {
					logger.warn("[Switchy] Unable to set current preset from data. Data may have been lost.");
				}

			if (presets.isEmpty() || getCurrentPreset() == null) {
				// Recover current data as "Default" preset
				setCurrentPreset(newPreset("default").getName());
			}
		}
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		modules.forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound listNbt = new NbtCompound();
		for (Preset preset : presets.values()) {
			listNbt.put(preset.getName(), preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		outNbt.putString(KEY_PRESET_CURRENT, getCurrentPresetName());
		return outNbt;
	}

	void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		list.forEach((e) -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null && modules.containsKey(id)) {
				modules.put(id, enabled);
			} else if (!silent) {
				logger.warn("[Switchy] Unable to toggle a module - Was a module unloaded?");
				logger.warn("[Switchy] NBT Element: " + e.asString());
			}
		});
	}

	public void importFromOther(Map<String, Preset> other) {
		// Don't process the current preset, it won't do anything
		other.remove(getCurrentPresetName());

		// Replace enabled modules for collisions
		other.forEach((name, preset) -> {
			if (presets.containsKey(name)) {
				preset.getModules().forEach((id, module) -> {
					presets.get(name).removeModule(id);
					presets.get(name).putModule(id, module);
				});
			}
		});

		// Add non-colliding presets
		other.forEach((name, preset) -> {
			if (!presets.containsKey(name)) {
				modules.forEach((id, enabled) -> {
					if (enabled && !preset.containsModule(id)) { // Add missing modules
						preset.putModule(id, moduleSupplier.apply(id));
					}
				});
				addPreset(preset);
			}
		});
	}

	void setCurrentPreset(String name) throws IllegalArgumentException {
		if (!presets.containsKey(name)) throw new IllegalArgumentException("Specified preset does not exist");
		currentPreset = presets.get(name);
	}

	public void addPreset(Preset preset) throws IllegalStateException {
		if (containsPreset(preset.getName())) throw new IllegalStateException("Specified preset already exists.");
		presets.put(preset.getName(), preset);
	}

	public Preset newPreset(String name) throws IllegalStateException {
		if (presets.containsKey(name)) throw new IllegalStateException("Specified preset already exists.");
		Preset newPreset = presetConstructor.apply(name, modules);
		presets.put(name, newPreset);
		return newPreset;
	}

	public void deletePreset(String name, boolean dryRun) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(name)) throw new IllegalArgumentException("Specified preset does not exist");
		if (getCurrentPresetName().equalsIgnoreCase(name)) throw new IllegalStateException("Specified preset is current");
		if (dryRun) return;
		presets.remove(name);
	}

	public void deletePreset(String name) throws IllegalArgumentException, IllegalStateException {
		deletePreset(name, false);
	}

	public void renamePreset(String oldName, String newName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(oldName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presets.containsKey(newName)) throw new IllegalStateException("Specified preset name already exists");
		Preset preset = presets.get(oldName);
		preset.setName(newName);
		presets.put(newName, preset);
		presets.remove(oldName);
	}

	public void disableModule(Identifier id, boolean dryRun) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!modules.get(id)) throw new IllegalStateException("Specified module is already disabled");
		if (dryRun) return;
		modules.put(id, false);
		presets.forEach((name, preset) -> preset.removeModule(id));
	}

	public void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		disableModule(id, false);
	}

	public void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (modules.get(id)) throw new IllegalStateException("Specified module is already enabled");
		modules.put(id, true);
		presets.values().forEach(preset -> preset.putModule(id, moduleSupplier.apply(id)));
	}

	// Current Preset Accessors

	@ApiStatus.Internal
	public Preset getCurrentPreset() {
		return currentPreset;
	}

	public String getCurrentPresetName() {
		return currentPreset.getName();
	}

	// Presets Accessors

	@ApiStatus.Internal
	public Map<String, Preset> getPresets() {
		return presets;
	}

	@ApiStatus.Internal
	public Preset getPreset(String name) {
		if (!presets.containsKey(name)) throw new IllegalArgumentException("Specified preset does not exist");
		return presets.get(name);
	}

	public List<String> getPresetNames() {
		return presets.keySet().stream().sorted().toList();
	}

	public boolean containsPreset(String name) {
		return presets.containsKey(name);
	}

	// Modules Accessors

	@ApiStatus.Internal
	public Map<Identifier, Boolean> getModules() {
		return modules;
	}

	public Map<String, Module> getAllOfModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!containsModule(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified module is disabled");
		Map<String, Module> outMap = new HashMap<>();
		presets.forEach((name, preset) -> outMap.put(name, preset.getModule(id)));
		return outMap;
	}

	public List<Identifier> getEnabledModules() {
		return modules.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
	}

	public List<Identifier> getDisabledModules() {
		return modules.entrySet().stream().filter((e) -> !e.getValue()).map(Map.Entry::getKey).toList();
	}

	public boolean containsModule(Identifier id) {
		return modules.containsKey(id);
	}

	public boolean isModuleEnabled(Identifier id) throws IllegalArgumentException {
		if (!containsModule(id)) throw new IllegalArgumentException("Specified module does not exist");
		return modules.get(id);
	}

	public List<String> getEnabledModuleNames() {
		return getEnabledModules().stream().map(Identifier::getPath).toList();
	}

	public MutableText getEnabledModuleText() {
		return getIdListText(getEnabledModules());
	}

	@Override
	public String toString() {
		return presets.keySet().toString();
	}
}

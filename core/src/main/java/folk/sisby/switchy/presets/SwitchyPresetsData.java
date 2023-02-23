package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static folk.sisby.switchy.util.Feedback.getIdText;

public class SwitchyPresetsData<Module extends SwitchySerializable, Preset extends SwitchyPresetData<Module>> implements SwitchySerializable {
	public final Map<String, Preset> presets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public final Map<Identifier, Boolean> modules;

	public final BiFunction<String, Map<Identifier, Boolean>, Preset> presetConstructor;
	public final Map<Identifier, Supplier<Module>> moduleSupplier;
	public final boolean forPlayer;
	public final Logger logger;

	public Preset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ENABLED = "enabled";
	public static final String KEY_PRESET_MODULE_DISABLED = "disabled";
	public static final String KEY_PRESET_LIST = "list";

	SwitchyPresetsData(Map<Identifier, Boolean> modules, BiFunction<String, Map<Identifier, Boolean>, Preset> presetConstructor, Map<Identifier, Supplier<Module>> moduleSupplier, boolean forPlayer, Logger logger) {
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
			Preset preset = presetConstructor.apply(key, modules);
			preset.fillFromNbt(listNbt.getCompound(key));
			try {
				addPreset(preset);
			} catch (IllegalStateException ignored) {
				logger.warn("[Switchy] Player data contained duplicate preset '{}'. Data may have been lost.", preset.name);
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
				addPreset(presetConstructor.apply("default", modules));
				setCurrentPreset("default");
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
			listNbt.put(preset.name, preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		if (currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.name);
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
		other.remove(currentPreset.name);

		// Replace enabled modules for collisions
		other.forEach((name, preset) -> {
			if (presets.containsKey(name)) {
				preset.modules.forEach((moduleId, module) -> {
					presets.get(name).modules.remove(moduleId);
					presets.get(name).modules.put(moduleId, module);
				});
			}
		});

		// Add non-colliding presets
		other.forEach((name, preset) -> {
			if (!presets.containsKey(name)) {
				modules.forEach((moduleId, enabled) -> {
					if (enabled && !preset.modules.containsKey(moduleId)) { // Add missing modules
						preset.modules.put(moduleId, moduleSupplier.get(moduleId).get());
					}
				});
				addPreset(preset);
			}
		});
	}

	void setCurrentPreset(String presetName) throws IllegalArgumentException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		currentPreset = presets.get(presetName);
	}

	public void addPreset(Preset preset) throws IllegalStateException {
		if (presets.containsKey(preset.name))
			throw new IllegalStateException("Specified preset already exists.");
		presets.put(preset.name, preset);
	}

	public void deletePreset(String presetName, boolean dryRun) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (currentPreset.name.equalsIgnoreCase(presetName)) throw new IllegalStateException("Specified preset is current");
		if (dryRun) return;
		presets.remove(presetName);
	}

	public void deletePreset(String presetName) throws IllegalArgumentException, IllegalStateException {
		deletePreset(presetName, false);
	}

	public void renamePreset(String oldName, String newName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(oldName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presets.containsKey(newName)) throw new IllegalStateException("Specified preset name already exists");
		Preset preset = presets.get(oldName);
		preset.name = newName;
		presets.put(newName, preset);
		presets.remove(oldName);
	}

	public void disableModule(Identifier id, boolean dryRun) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!modules.get(id)) throw new IllegalStateException("Specified module is already disabled");
		if (dryRun) return;
		modules.put(id, false);
		presets.forEach((name, preset) -> preset.modules.remove(id));
	}

	public void disableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		disableModule(id, false);
	}

	public void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (modules.get(id)) throw new IllegalStateException("Specified module is already enabled");
		modules.put(id, true);
		presets.values().forEach(preset -> preset.modules.put(id, moduleSupplier.get(id).get()));
	}

	public Preset getCurrentPreset() {
		return currentPreset;
	}

	public List<String> getPresetNames() {
		return presets.keySet().stream().sorted().toList();
	}

	public List<Identifier> getEnabledModules() {
		return modules.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
	}

	public List<String> getEnabledModuleNames() {
		return getEnabledModules().stream().map(Identifier::getPath).toList();
	}

	public MutableText getEnabledModuleText() {
		return getIdText(getEnabledModules());
	}

	public List<Identifier> getDisabledModules() {
		return modules.entrySet().stream().filter((e) -> !e.getValue()).map(Map.Entry::getKey).toList();
	}

	@Override
	public String toString() {
		return presets.keySet().toString();
	}
}

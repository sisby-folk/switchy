package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchySwitchEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static folk.sisby.switchy.Switchy.S2C_SWITCH;
import static folk.sisby.switchy.util.Feedback.getIdText;

public class SwitchyPresets {

	private final Map<String, SwitchyPreset> presetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public final Map<Identifier, Boolean> modules;
	private SwitchyPreset currentPreset;

	public static final String KEY_PRESET_CURRENT = "current";
	public static final String KEY_PRESET_MODULE_ENABLED = "enabled";
	public static final String KEY_PRESET_MODULE_DISABLED = "disabled";
	public static final String KEY_PRESET_LIST = "list";

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();

		NbtList enabledList = new NbtList();
		NbtList disabledList = new NbtList();

		this.modules.forEach((key, value) -> {
			if (value) enabledList.add(NbtString.of(key.toString()));
			if (!value) disabledList.add(NbtString.of(key.toString()));
		});

		outNbt.put(KEY_PRESET_MODULE_ENABLED, enabledList);
		outNbt.put(KEY_PRESET_MODULE_DISABLED, disabledList);

		NbtCompound listNbt = new NbtCompound();
		for (SwitchyPreset preset : presetMap.values()) {
			listNbt.put(preset.presetName, preset.toNbt());
		}
		outNbt.put(KEY_PRESET_LIST, listNbt);

		if (this.currentPreset != null) outNbt.putString(KEY_PRESET_CURRENT, currentPreset.presetName);
		return outNbt;
	}

	public static SwitchyPresets fromNbt(NbtCompound nbt, @Nullable PlayerEntity player) {
		SwitchyPresets outPresets = new SwitchyPresets();

		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE), true, player == null);
		outPresets.toggleModulesFromNbt(nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE), false, player == null);

		NbtCompound listNbt = nbt.getCompound(KEY_PRESET_LIST);
		for (String key : listNbt.getKeys()) {
			SwitchyPreset preset = SwitchyPreset.fromNbt(key, listNbt.getCompound(key), outPresets.modules);
			if (!outPresets.addPreset(preset)) {
				Switchy.LOGGER.warn("Switchy: Player data contained duplicate preset. Data may have been lost.");
			}
		}

		if (player != null) {
			if (nbt.contains(KEY_PRESET_CURRENT) && !outPresets.setCurrentPreset(nbt.getString(KEY_PRESET_CURRENT))) {
				Switchy.LOGGER.warn("Switchy: Unable to set current preset from data. Data may have been lost.");
			}

			if (outPresets.presetMap.isEmpty() || outPresets.getCurrentPreset() == null) {
				// Recover current data as "Default" preset
				outPresets.addPreset(new SwitchyPreset("default", outPresets.modules));
				outPresets.setCurrentPreset("default");
			}
		}

		return outPresets;
	}

	public boolean importFromOther(SwitchyPresets other, List<Identifier> importModules) {
		if (other.getPresetNames().stream().noneMatch((name) -> this.getPresetNames().contains(name))) {
			// Remove modules that shouldn't be imported
			this.modules.forEach((key, enabled) -> {
				if (other.modules.get(key) && (!importModules.contains(key) || !enabled)) {
					other.disableModule(key);
				}
			});

			// Re-enable missing empty modules
			this.modules.forEach((key, enabled) -> {
				if (enabled && !other.modules.get(key)) {
					other.enableModule(key);
				}
			});

			other.presetMap.values().forEach(this::addPreset);
			return true;
		} else {
			return false;
		}
	}

	private void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		list.forEach((e) -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null && this.modules.containsKey(id)) {
				this.modules.put(id, enabled);
			} else if (!silent) {
				Switchy.LOGGER.warn("Switchy: Unable to toggle a module - Was a module unloaded?");
				Switchy.LOGGER.warn("Switchy: NBT Element: " + e.asString());
			}
		});
	}

	private SwitchyPresets() {
		this.modules = Switchy.MODULE_SUPPLIERS.entrySet().stream()
				.filter(e -> e.getValue().get() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Switchy.MODULE_INFO.get(e.getKey()).isDefault()));
	}

	private boolean setCurrentPreset(String presetName) {
		if (this.presetMap.containsKey(presetName)) {
			this.currentPreset = this.presetMap.get(presetName);
			return true;
		} else {
			return false;
		}
	}

	public @Nullable String switchCurrentPreset(ServerPlayerEntity player, String presetName) {
		if (this.presetMap.containsKey(presetName)) {
			SwitchyPreset newPreset = this.presetMap.get(presetName);

			// Perform Switch
			this.currentPreset.updateFromPlayer(player, newPreset.presetName);
			newPreset.applyToPlayer(player);

			// Fire Events
			SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
					player.getUuid(), newPreset.presetName, Objects.toString(this.currentPreset, null), getEnabledModuleNames()
			);
			SwitchyEvents.fireSwitch(switchEvent);
			if (ServerPlayNetworking.canSend(player, S2C_SWITCH)) {
				ServerPlayNetworking.send(player, S2C_SWITCH, PacketByteBufs.create().writeNbt(switchEvent.toNbt()));
			}

			this.currentPreset = newPreset;
			return newPreset.presetName;
		} else {
			return null;
		}
	}

	public void saveCurrentPreset(PlayerEntity player) {
		if (this.currentPreset != null) this.currentPreset.updateFromPlayer(player, null);
	}

	public SwitchyPreset getCurrentPreset() {
		return currentPreset;
	}

	public boolean addPreset(SwitchyPreset preset) {
		if (presetMap.containsKey(preset.presetName)) {
			return false;
		} else {
			presetMap.put(preset.presetName, preset);
			return true;
		}
	}

	@Override
	public String toString() {
		return presetMap.keySet().toString();
	}

	public List<String> getPresetNames() {
		return presetMap.keySet().stream().sorted().toList();
	}

	public void deletePreset(String presetName) {
		if (this.presetMap.containsKey(presetName) && !this.currentPreset.toString().equalsIgnoreCase(presetName)) {
			this.presetMap.remove(presetName);
		} else {
			throw new IllegalArgumentException("Switchy can't delete that preset");
		}
	}

	public void renamePreset(String oldName, String newName) {
		if (this.presetMap.containsKey(oldName) && !this.presetMap.containsKey(newName)) {
			SwitchyPreset preset = this.presetMap.get(oldName);
			preset.presetName = newName;
			this.presetMap.put(newName, preset);
			this.presetMap.remove(oldName);
		} else {
			throw new IllegalArgumentException("Switchy rename not valid");
		}
	}

	public boolean containsPreset(String presetName) {
		return this.presetMap.containsKey(presetName); // Case Insensitive
	}

	public void disableModule(Identifier id) {
		if (this.modules.containsKey(id)) {
			this.modules.put(id, false);
			presetMap.forEach((name, preset) -> preset.compatModules.remove(id));
		} else {
			throw new IllegalArgumentException("Switchy module doesn't exist");
		}
	}

	public void enableModule(Identifier id) {
		if (this.modules.containsKey(id)) {
			this.modules.put(id, true);
			for (SwitchyPreset preset : presetMap.values()) {
				PresetModule module = Switchy.MODULE_SUPPLIERS.get(id).get();
				preset.compatModules.put(id, module);
			}
		} else {
			throw new IllegalArgumentException("Switchy module doesn't exist");
		}
	}

	public List<Identifier> getEnabledModules() {
		return this.modules.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
	}

	public List<String> getEnabledModuleNames() {
		return getEnabledModules().stream().map(Identifier::getPath).toList();
	}

	public MutableText getEnabledModuleText() {
		return getIdText(getEnabledModules());
	}

	public List<Identifier> getDisabledModules() {
		return this.modules.entrySet().stream().filter((e) -> !e.getValue()).map(Map.Entry::getKey).toList();
	}
}

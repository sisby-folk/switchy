package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyModules;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SwitchyPresets extends SwitchyPresetsData<SwitchyModule, SwitchyPreset> {
	public SwitchyPresets(boolean forPlayer) {
		super(SwitchyModules.MODULE_SUPPLIERS.entrySet().stream()
				.filter(e -> e.getValue().get() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> SwitchyModules.MODULE_INFO.get(e.getKey()).isDefault())), SwitchyPreset::new, forPlayer, Switchy.LOGGER);
	}

	public void importFromOther(@Nullable ServerPlayerEntity player, SwitchyPresets other) {
		importFromOther(player, other.presets);
	}

	public void importFromOther(@Nullable ServerPlayerEntity player, Map<String, SwitchyPreset> other) {
		// Replace enabled modules for colliding current preset
		if (other.containsKey(this.currentPreset.presetName) && player != null) {
			other.get(this.currentPreset.presetName).modules.forEach((moduleId, module) ->
					duckCurrentModule(player, moduleId, (duckedModule) -> duckedModule.fillFromNbt(duckedModule.toNbt()))
			);
		}
		other.remove(currentPreset.presetName);

		// Replace enabled modules for collisions
		other.entrySet().stream().filter(e -> presets.containsKey(e.getKey())).forEach(e -> e.getValue().modules.forEach((moduleId, module) -> {
			presets.get(e.getKey()).modules.remove(moduleId);
			presets.get(e.getKey()).modules.put(moduleId, module);
		}));

		// Add non-colliding presets
		other.forEach((name, preset) -> {
			if (!presets.containsKey(name)) {
				modules.forEach((moduleId, enabled) -> {
					if (enabled && !preset.modules.containsKey(moduleId)) { // Add missing modules
						preset.modules.put(moduleId, SwitchyModules.MODULE_SUPPLIERS.get(moduleId).get());
					}
				});
				addPreset(preset);
			}
		});
	}

	public String switchCurrentPreset(ServerPlayerEntity player, String presetName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presetName.equalsIgnoreCase(Objects.toString(currentPreset, "")))
			throw new IllegalStateException("Specified preset is already current");

		SwitchyPreset newPreset = presets.get(presetName);

		// Perform Switch
		currentPreset.updateFromPlayer(player, newPreset.presetName);
		newPreset.applyToPlayer(player);

		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				player.getUuid(), newPreset.presetName, Objects.toString(currentPreset, null), getEnabledModuleNames()
		);
		currentPreset = newPreset;
		SwitchyEvents.SWITCH.invoker().onSwitch(player, switchEvent);

		return currentPreset.presetName;
	}

	public void saveCurrentPreset(ServerPlayerEntity player) {
		if (currentPreset != null) currentPreset.updateFromPlayer(player, null);
	}

	public void duckCurrentModule(ServerPlayerEntity player, Identifier moduleId, Consumer<SwitchyModule> mutator) throws IllegalArgumentException, IllegalStateException {
		if (currentPreset == null) throw new IllegalStateException("Specified player has no current preset");
		if (!modules.containsKey(moduleId)) throw new IllegalArgumentException("Specified module does not exist");
		if (!modules.get(moduleId)) throw new IllegalStateException("Specified module is not enabled");
		SwitchyModule module = currentPreset.modules.get(moduleId);
		module.updateFromPlayer(player, null);
		mutator.accept(module);
		module.applyToPlayer(player);
	}

	public void enableModule(Identifier id) throws IllegalArgumentException, IllegalStateException {
		if (!modules.containsKey(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (modules.get(id)) throw new IllegalStateException("Specified module is already enabled");
		modules.put(id, true);
		presets.values().forEach(preset -> preset.modules.put(id, SwitchyModules.MODULE_SUPPLIERS.get(id).get()));
	}
}

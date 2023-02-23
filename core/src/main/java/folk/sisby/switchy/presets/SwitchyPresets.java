package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
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
		super(
				SwitchyModuleRegistry.SUPPLIERS.entrySet().stream()
						.filter(e -> e.getValue().get() != null)
						.collect(Collectors.toMap(Map.Entry::getKey, e -> SwitchyModuleRegistry.INFO.get(e.getKey()).isDefault())),
				SwitchyPreset::new,
				SwitchyModuleRegistry.SUPPLIERS,
				forPlayer,
				Switchy.LOGGER
		);
	}

	public void importFromOther(@Nullable ServerPlayerEntity player, SwitchyPresets other) {
		importFromOther(player, other.presets);
	}

	public void importFromOther(@Nullable ServerPlayerEntity player, Map<String, SwitchyPreset> other) {
		// Replace enabled modules for colliding current preset
		if (other.containsKey(this.currentPreset.name) && player != null) {
			other.get(this.currentPreset.name).modules.forEach((moduleId, module) ->
					duckCurrentModule(player, moduleId, (duckedModule) -> duckedModule.fillFromNbt(duckedModule.toNbt()))
			);
		}
		importFromOther(other);
	}

	public String switchCurrentPreset(ServerPlayerEntity player, String presetName) throws IllegalArgumentException, IllegalStateException {
		if (!presets.containsKey(presetName)) throw new IllegalArgumentException("Specified preset does not exist");
		if (presetName.equalsIgnoreCase(Objects.toString(currentPreset, "")))
			throw new IllegalStateException("Specified preset is already current");

		SwitchyPreset nextPreset = presets.get(presetName);

		// Perform Switch
		currentPreset.updateFromPlayer(player, nextPreset.name);
		nextPreset.applyToPlayer(player);

		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				player.getUuid(), nextPreset.name, Objects.toString(currentPreset, null), getEnabledModuleNames()
		);
		currentPreset = nextPreset;
		SwitchyEvents.SWITCH.invoker().onSwitch(player, switchEvent);

		return currentPreset.name;
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
}

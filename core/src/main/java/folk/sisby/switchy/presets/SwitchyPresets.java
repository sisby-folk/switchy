package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Consumer;

public class SwitchyPresets extends SwitchyPresetsData<SwitchyModule, SwitchyPreset> {
	public SwitchyPresets(boolean forPlayer) {
		super(
				SwitchyModuleRegistry.getModuleDefaults(),
				SwitchyPreset::new,
				SwitchyModuleRegistry::supplyModule,
				forPlayer,
				Switchy.LOGGER
		);
	}

	public void importFromOther(ServerPlayerEntity player, Map<String, SwitchyPreset> other) {
		// Replace enabled modules for colliding current preset
		if (other.containsKey(getCurrentPresetName())) {
			other.get(getCurrentPresetName()).getModules().forEach((id, module) ->
					duckCurrentModule(player, id, (duckedModule) -> duckedModule.fillFromNbt(duckedModule.toNbt()))
			);
		}
		importFromOther(other);
	}

	public void importFromOther(ServerPlayerEntity player, SwitchyPresets other) {
		importFromOther(player, other.getPresets());
	}

	public String switchCurrentPreset(ServerPlayerEntity player, String name) throws IllegalArgumentException, IllegalStateException {
		if (!containsPreset(name)) throw new IllegalArgumentException("Specified preset does not exist");
		if (getCurrentPresetName().equalsIgnoreCase(name)) throw new IllegalStateException("Specified preset is already current");

		SwitchyPreset nextPreset = getPreset(name);

		// Perform Switch
		getCurrentPreset().updateFromPlayer(player, nextPreset.getName());
		nextPreset.applyToPlayer(player);

		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				player.getUuid(), nextPreset.getName(), getCurrentPresetName(), getEnabledModuleNames()
		);
		setCurrentPreset(nextPreset.getName());
		SwitchyEvents.SWITCH.invoker().onSwitch(player, switchEvent);

		return getCurrentPresetName();
	}

	public void saveCurrentPreset(ServerPlayerEntity player) {
		getCurrentPreset().updateFromPlayer(player, null);
	}

	/**
	 * Allows you to modify the data associated with the current preset for a specified module, by saving it, mutating it, then loading it all in one swoop.
	 **/
	public void duckCurrentModule(ServerPlayerEntity player, Identifier id, Consumer<SwitchyModule> mutator) throws IllegalArgumentException, IllegalStateException {
		if (!containsModule(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified module is not enabled");
		SwitchyModule module = getCurrentPreset().getModule(id);
		module.updateFromPlayer(player, null);
		mutator.accept(module);
		module.applyToPlayer(player);
	}
}

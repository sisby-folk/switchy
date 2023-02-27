package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Sisby folk
 * @see SwitchyPresets
 * @since 1.0.0
 */
public class SwitchyPresetsImpl extends SwitchyPresetsDataImpl<SwitchyModule, SwitchyPreset> implements SwitchyPresets {
	/**
	 * @param forPlayer whether the presets object is "for a player" - affects recovering lost presets, and logging failures.
	 */
	public SwitchyPresetsImpl(boolean forPlayer) {
		super(
				SwitchyModuleRegistry.getModuleDefaults(),
				SwitchyPresetImpl::new,
				SwitchyModuleRegistry::supplyModule,
				forPlayer,
				Switchy.LOGGER
		);
	}

	@Override
	public void importFromOther(ServerPlayerEntity player, Map<String, SwitchyPreset> other) {
		// Replace enabled modules for colliding current preset
		if (other.containsKey(getCurrentPresetName())) {
			other.get(getCurrentPresetName()).getModules().forEach((id, module) ->
					duckCurrentModule(player, id, (duckedModule) -> duckedModule.fillFromNbt(duckedModule.toNbt()))
			);
		}
		importFromOther(other);
	}

	@Override
	public void importFromOther(ServerPlayerEntity player, SwitchyPresets other) {
		importFromOther(player, other.getPresets());
	}

	@Override
	public String switchCurrentPreset(ServerPlayerEntity player, String name) throws IllegalArgumentException, IllegalStateException {
		if (!containsPreset(name)) throw new IllegalArgumentException("Specified preset does not exist");
		if (getCurrentPresetName().equalsIgnoreCase(name))
			throw new IllegalStateException("Specified preset is already current");

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

	@Override
	public void saveCurrentPreset(ServerPlayerEntity player) {
		getCurrentPreset().updateFromPlayer(player, null);
	}

	@Override
	public void duckCurrentModule(ServerPlayerEntity player, Identifier id, Consumer<SwitchyModule> mutator) throws IllegalArgumentException, IllegalStateException {
		if (!containsModule(id)) throw new IllegalArgumentException("Specified module does not exist");
		if (!isModuleEnabled(id)) throw new IllegalStateException("Specified module is not enabled");
		SwitchyModule module = getCurrentPreset().getModule(id);
		module.updateFromPlayer(player, null);
		mutator.accept(module);
		module.applyToPlayer(player);
	}
}

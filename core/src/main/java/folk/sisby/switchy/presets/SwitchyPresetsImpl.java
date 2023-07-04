package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.exception.ClassNotAssignableException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.exception.PresetNotFoundException;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static folk.sisby.switchy.Switchy.LOGGER;

/**
 * @author Sisby folk
 * @see SwitchyPresets
 * @since 1.0.0
 */
public class SwitchyPresetsImpl extends SwitchyPresetsDataImpl<SwitchyModule, SwitchyPreset> implements SwitchyPresets {
	/**
	 * Constructs an instance of the object.
	 *
	 * @param forPlayer whether the presets object is "for a player" - affects recovering lost presets, and logging failures.
	 */
	public SwitchyPresetsImpl(boolean forPlayer) {
		super(
				SwitchyModuleRegistry.getModuleDefaults(),
				forPlayer,
				Switchy.LOGGER
		);
	}

	@Override
	public SwitchyPreset constructPreset(String name, Map<Identifier, Boolean> modules) {
		return new SwitchyPresetImpl(name, modules);
	}

	@Override
	public SwitchyModule supplyModule(Identifier id) {
		return SwitchyModuleRegistry.supplyModule(id);
	}

	@Override
	public @Nullable SwitchySerializable supplyModuleConfig(Identifier id) {
		return SwitchyModuleRegistry.generateModuleConfig(id);
	}

	@Override
	public void enableModule(ServerPlayerEntity player, Identifier id) throws ModuleNotFoundException, IllegalStateException {
		super.enableModuleAndReturn(id).forEach((module) -> module.onEnable(player));
	}

	@Override
	public void disableModule(ServerPlayerEntity player, Identifier id, boolean dryRun) throws ModuleNotFoundException, IllegalStateException {
		Map<String, SwitchyModule> modules = getAllOfModule(id);
		disableModule(id, dryRun);
		modules.forEach((name, module) -> module.onDelete(player, true));
	}

	@Override
	public void disableModule(ServerPlayerEntity player, Identifier id) throws ModuleNotFoundException, IllegalStateException {
		disableModule(player, id, false);
	}

	@Override
	public void deletePreset(ServerPlayerEntity player, String name, boolean dryRun) throws PresetNotFoundException, IllegalStateException {
		Map<Identifier, SwitchyModule> modules = getPreset(name).getModules();
		deletePreset(name, dryRun);
		modules.forEach((id, module) -> module.onDelete(player, false));
	}

	@Override
	public void deletePreset(ServerPlayerEntity player, String name) throws PresetNotFoundException, IllegalStateException {
		deletePreset(player, name, false);
	}

	@Override
	public void importFromOther(ServerPlayerEntity player, Map<String, SwitchyPreset> other) {
		other.forEach((name, otherPreset) -> {
			if (getPresets().containsKey(name)) {
				mutatePresetOfModules(player, name, (id, module) -> {
					if (otherPreset.containsModule(id)) module.fillFromNbt(otherPreset.getModule(id).toNbt());
				});
			} else {
				getEnabledModules().forEach((id) -> {
					if (!otherPreset.containsModule(id)) { // Add missing modules
						otherPreset.putModule(id, SwitchyModuleRegistry.supplyModule(id));
					}
				});
				otherPreset.getModules().forEach((id, module) -> { // Remove disabled/unregistered modules
					if (!containsModule(id) || !isModuleEnabled(id)) {
						otherPreset.removeModule(id);
					}
				});
				addPreset(otherPreset);
			}
		});
	}

	@Override
	public void importFromOther(ServerPlayerEntity player, SwitchyPresets other) {
		importFromOther(player, other.getPresets());
	}

	@Override
	public String switchCurrentPreset(ServerPlayerEntity player, String name) throws PresetNotFoundException, IllegalStateException {
		if (!containsPreset(name)) throw new PresetNotFoundException();
		if (getCurrentPresetName().equalsIgnoreCase(name))
			throw new IllegalStateException("Specified preset is current");

		SwitchyPreset nextPreset = getPreset(name);

		// Perform Switch
		SwitchyPreset oldPreset = getCurrentPreset();
		oldPreset.updateFromPlayer(player, nextPreset.getName());
		nextPreset.applyToPlayer(player);

		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				player.getUuid(), nextPreset.getName(), getCurrentPresetName(), getEnabledModuleNames()
		);
		setCurrentPreset(nextPreset.getName());
		SwitchyEvents.SWITCH.invoker().onSwitch(player, switchEvent);
		LOGGER.info("[Switchy] Player switch: '" + oldPreset.getName() + "' -> '" + getCurrentPresetName() + "' [" + player.getGameProfile().getName() + "]");

		return getCurrentPresetName();
	}

	@Override
	public void saveCurrentPreset(ServerPlayerEntity player) {
		getCurrentPreset().updateFromPlayer(player, null);
	}

	private <ModuleType extends SwitchyModule> void mutateModule(ServerPlayerEntity player, String name, ModuleType module, Consumer<ModuleType> mutator) {
		if (name.equalsIgnoreCase(getCurrentPresetName())) module.updateFromPlayer(player, null);
		mutator.accept(module);
		if (name.equalsIgnoreCase(getCurrentPresetName())) module.applyToPlayer(player);
	}

	@Override
	public <ModuleType extends SwitchyModule> void mutateModule(ServerPlayerEntity player, String name, Identifier id, Consumer<ModuleType> mutator, Class<ModuleType> clazz) throws PresetNotFoundException, ModuleNotFoundException, ClassNotAssignableException, IllegalStateException {
		mutateModule(player, name, getModule(name, id, clazz), mutator);
	}

	@Override
	public <ModuleType extends SwitchyModule> void mutateAllOfModule(ServerPlayerEntity player, Identifier id, BiConsumer<String, ModuleType> mutator, Class<ModuleType> clazz) throws ModuleNotFoundException, ClassNotAssignableException, IllegalStateException {
		getAllOfModule(id, clazz).forEach((name, module) -> mutateModule(player, name, module, m -> mutator.accept(name, m)));
	}

	@Override
	public void mutatePresetOfModules(ServerPlayerEntity player, String name, BiConsumer<Identifier, SwitchyModule> mutator) throws PresetNotFoundException {
		getPreset(name).getModules().forEach((id, module) -> mutateModule(player, name, module, m -> mutator.accept(id, m)));
	}
}

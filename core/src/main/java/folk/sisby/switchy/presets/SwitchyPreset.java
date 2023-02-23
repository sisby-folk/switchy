package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyModules;
import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchyPreset extends SwitchyPresetData<SwitchyModule> {
	public SwitchyPreset(String name, Map<Identifier, Boolean> moduleToggles) {
		super(name, moduleToggles.entrySet().stream()
				.filter(Map.Entry::getValue)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> SwitchyModules.MODULE_SUPPLIERS.get(e.getKey()).get())));
	}

	public void updateFromPlayer(ServerPlayerEntity player, String nextPreset) {
		this.modules.forEach((id, module) -> {
			try {
				module.updateFromPlayer(player, nextPreset);
			} catch (Exception ex) {
				Switchy.LOGGER.error("[Switchy] Module " + id + " failed to update! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
		});
	}

	private static void tryApplyModule(Map<Identifier, SwitchyModule> modules, Identifier id, ServerPlayerEntity player, Set<Identifier> registeredModules) {
		if (!registeredModules.contains(id) && modules.containsKey(id)) {
			try {
				SwitchyModule module = modules.get(id);
				SwitchyModules.MODULE_INFO.get(id).applyDependencies().forEach((depId) -> tryApplyModule(modules, depId, player, registeredModules));
				module.applyToPlayer(player);
			} catch (Exception ex) {
				Switchy.LOGGER.error("[Switchy] Module " + id + " failed to apply! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
			registeredModules.add(id);
		}
	}

	public void applyToPlayer(ServerPlayerEntity player) {
		this.modules.forEach((id, module) -> tryApplyModule(modules, id, player, new HashSet<>()));
	}
}

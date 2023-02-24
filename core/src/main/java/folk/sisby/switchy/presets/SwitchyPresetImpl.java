package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SwitchyPresetImpl extends SwitchyPresetDataImpl<SwitchyModule> implements SwitchyPreset {
	public SwitchyPresetImpl(String name, Map<Identifier, Boolean> modules) {
		super(name, modules, SwitchyModuleRegistry::supplyModule);
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, String nextPreset) {
		getModules().forEach((id, module) -> {
			try {
				module.updateFromPlayer(player, nextPreset);
			} catch (Exception ex) {
				Switchy.LOGGER.error("[Switchy] Module " + id + " failed to update! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
		});
	}

	static void tryApplyModule(Map<Identifier, SwitchyModule> modules, Identifier id, ServerPlayerEntity player, Set<Identifier> registeredModules) {
		if (!registeredModules.contains(id) && modules.containsKey(id)) {
			try {
				SwitchyModule module = modules.get(id);
				SwitchyModuleRegistry.getApplyDependencies(id).forEach((depId) -> tryApplyModule(modules, depId, player, registeredModules));
				module.applyToPlayer(player);
			} catch (Exception ex) {
				Switchy.LOGGER.error("[Switchy] Module " + id + " failed to apply! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
			registeredModules.add(id);
		}
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		getModules().forEach((id, module) -> tryApplyModule(getModules(), id, player, new HashSet<>()));
	}
}

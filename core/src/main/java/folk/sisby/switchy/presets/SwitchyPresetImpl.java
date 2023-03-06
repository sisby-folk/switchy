package folk.sisby.switchy.presets;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Sisby folk
 * @see SwitchyPreset
 * @since 1.0.0
 */
public class SwitchyPresetImpl extends SwitchyPresetDataImpl<SwitchyModule> implements SwitchyPreset {
	/**
	 * Constructs an instance of the object.
	 *
	 * @param name    the desired name for the new preset.
	 * @param modules the enabled status of modules from the presets object.
	 * @throws IllegalArgumentException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 */
	public SwitchyPresetImpl(String name, Map<Identifier, Boolean> modules) throws IllegalArgumentException {
		super(name, modules, SwitchyModuleRegistry::supplyModule);
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

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		getModules().forEach((id, module) -> tryApplyModule(getModules(), id, player, new HashSet<>()));
	}
}

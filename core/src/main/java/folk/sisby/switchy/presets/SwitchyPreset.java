package folk.sisby.switchy.presets;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.PresetModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public class SwitchyPreset {
	public final Map<Identifier, PresetModule> modules;

	public String presetName;

	public SwitchyPreset(String name, Map<Identifier, Boolean> moduleToggles) {
		this.presetName = name;
		this.modules = moduleToggles.entrySet().stream()
				.filter(Map.Entry::getValue)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Switchy.MODULE_SUPPLIERS.get(e.getKey()).get()));
	}

	public NbtCompound toNbt(boolean displayOnly) {
		NbtCompound outNbt = new NbtCompound();
		this.modules.forEach((id, module) -> outNbt.put(id.toString(), module.toNbt(displayOnly)));
		return outNbt;
	}

	public NbtCompound toNbt() {
		return toNbt(false);
	}

	public static SwitchyPreset fromNbt(String presetName, NbtCompound nbt, Map<Identifier, Boolean> moduleToggles) {
		SwitchyPreset outPreset = new SwitchyPreset(presetName, moduleToggles);
		outPreset.modules.forEach((id, module) -> module.fillFromNbt(nbt.getCompound(id.toString())));
		return outPreset;
	}

	public void updateFromPlayer(PlayerEntity player, String nextPreset) {
		this.modules.forEach((id, module) -> {
			try {
				module.updateFromPlayer(player, nextPreset);
			} catch (Exception ex) {
				Switchy.LOGGER.error("Switchy: Module " + id + " failed to update! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
		});
	}

	private static void tryApplyModule(Map<Identifier, PresetModule> modules, Identifier id, PlayerEntity player, Set<Identifier> registeredModules) {
		if (!registeredModules.contains(id) && modules.containsKey(id)) {
			try {
				PresetModule module = modules.get(id);
				Switchy.MODULE_INFO.get(id).applyDependencies().forEach((depId) -> tryApplyModule(modules, depId, player, registeredModules));
				module.applyToPlayer(player);
			} catch (Exception ex) {
				Switchy.LOGGER.error("Switchy: Module " + id + " failed to apply! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
			registeredModules.add(id);
		}
	}

	public void applyToPlayer(PlayerEntity player) {
		this.modules.forEach((id, module) -> tryApplyModule(modules, id, player, new HashSet<>()));
	}

	@Override
	public String toString() {
		return presetName;
	}
}

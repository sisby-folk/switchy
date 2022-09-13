package folk.sisby.switchy;

import folk.sisby.switchy.api.PresetModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.stream.Collectors;

public class SwitchyPreset {
	public final Map<Identifier, PresetModule> compatModules;

	public String presetName;

	public SwitchyPreset(String name, Map<Identifier, Boolean> moduleToggles) {
		this.presetName = name;
		this.compatModules = Switchy.COMPAT_REGISTRY.entrySet().stream()
				.filter(pair -> moduleToggles.get(pair.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
	}

	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		this.compatModules.forEach((id, module) -> outNbt.put(id.toString(), module.toNbt()));
		return outNbt;
	}

	public static SwitchyPreset fromNbt(String presetName, NbtCompound nbt, Map<Identifier, Boolean> moduleToggles) {
		SwitchyPreset outPreset = new SwitchyPreset(presetName, moduleToggles);
		outPreset.compatModules.forEach((id, module) -> module.fillFromNbt(nbt.getCompound(id.toString())));
		return outPreset;
	}

	public void updateFromPlayer(PlayerEntity player) {
		this.compatModules.forEach((id, module) -> {
			try {
				module.updateFromPlayer(player);
			} catch (Exception ex) {
				Switchy.LOGGER.error("Switchy: Module " + module.getId() + " failed to update! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
		});
	}

	public void applyToPlayer(PlayerEntity player) {
		this.compatModules.forEach((id, module) -> {
			try {
				module.applyToPlayer(player);
			} catch (Exception ex) {
				Switchy.LOGGER.error("Switchy: Module " + module.getId() + " failed to apply! Error:");
				Switchy.LOGGER.error(ex.toString());
			}
		});
	}

	@Override
	public String toString() {
		return presetName;
	}
}

package folk.sisby.switchy;

import com.unascribed.drogtor.DrogtorPlayer;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SwitchyPresets {

	private final Map<String, SwitchyPreset> presetMap;
	private final PlayerEntity player;
	@Nullable private SwitchyPreset currentPreset;

	public NbtElement toNbt() {
		NbtList outList = new NbtList();
		for (SwitchyPreset preset : presetMap.values()) {
			outList.add(preset.toNbt());
		}
		return outList;
	}

	public static SwitchyPresets fromNbt(PlayerEntity player, NbtList nbtList) {
		SwitchyPresets outPresets = SwitchyPresets.fromEmpty(player);
		for (NbtElement item : nbtList) {
			if (item.getType() == NbtType.LIST && item instanceof NbtList list) {
				SwitchyPreset preset = SwitchyPreset.fromNbt(list);
				if (!outPresets.addPreset(preset)) {
					Switchy.LOGGER.warn("Player data contained duplicate preset. Data may have been lost.");
				}
			}
		}
		return outPresets;
	}

	public static SwitchyPresets fromEmpty(PlayerEntity player) {
		return new SwitchyPresets(player, new HashMap<>());
	}

	private SwitchyPresets(PlayerEntity player, Map<String, SwitchyPreset> presetMap) {
		this.player = player;
		this.presetMap = presetMap;
	}

	public boolean setCurrentPreset(String presetName){
		if (this.presetMap.containsKey(presetName)) {
			SwitchyPreset newPreset = this.presetMap.get(presetName);
			this.switchPreset(currentPreset, newPreset);
			this.currentPreset = newPreset;
			return true;
		} else {
			return false;
		}
	}

	private void switchPreset(SwitchyPreset oldPreset, SwitchyPreset newPreset) {
		if (oldPreset != null) {
			// Preserve current values into preset before swapping
			oldPreset.updateFromPlayer(player);
		}
		newPreset.applyToPlayer(player);
	}

	public @Nullable SwitchyPreset getCurrentPreset(){
		return currentPreset;
	}

	public boolean addPreset(SwitchyPreset preset) {
		if (presetMap.containsKey(preset.presetName)) {
			return false;
		} else {
			presetMap.put(preset.presetName, preset);
			return true;
		}
	}

	@Override
	public String toString() {
		return presetMap.keySet().toString();
	}

	public boolean deletePreset(String presetName) {
		if (this.presetMap.containsKey(presetName)) {
			if (Objects.equals(Objects.toString(this.currentPreset, null), presetName)) {
				this.currentPreset = null;
			}
			this.presetMap.remove(presetName);
			return true;
		} else {
			return false;
		}
	}
}

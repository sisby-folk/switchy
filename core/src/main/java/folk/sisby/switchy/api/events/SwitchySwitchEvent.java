package folk.sisby.switchy.api.events;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SwitchySwitchEvent {
	public final UUID player;
	public final @Nullable String previousPreset; // Null previous preset means "joined"
	public final @Nullable String currentPreset; // Null current preset means "disconnected"
	public final List<String> enabledModules;

	private static final String KEY_PLAYER = "player";
	private static final String KEY_CURRENT_PRESET = "currentName";
	private static final String KEY_PREVIOUS_PRESET = "previousName";
	private static final String KEY_ENABLED_MODULES = "enabledModules";

	public SwitchySwitchEvent(UUID player, @Nullable String currentPreset, @Nullable String previousPreset, List<String> enabledModules) {
		this.player = player;
		this.previousPreset = previousPreset;
		this.currentPreset = currentPreset;
		this.enabledModules = enabledModules;
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid(KEY_PLAYER, player);
		nbt.putString(KEY_CURRENT_PRESET, currentPreset);
		NbtList nbtModules = new NbtList();
		nbtModules.addAll(enabledModules.stream().map(NbtString::of).toList());
		nbt.put(KEY_ENABLED_MODULES, nbtModules);
		if(previousPreset != null) nbt.putString(KEY_PREVIOUS_PRESET, previousPreset);
		return nbt;
	}

	public static SwitchySwitchEvent fromNbt(NbtCompound nbt) {
		return new SwitchySwitchEvent(nbt.getUuid(KEY_PLAYER), nbt.getString(KEY_CURRENT_PRESET), nbt.contains(KEY_PREVIOUS_PRESET, NbtElement.STRING_TYPE) ? nbt.getString(KEY_PREVIOUS_PRESET) : null, nbt.getList(KEY_ENABLED_MODULES, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).toList());
	}
}

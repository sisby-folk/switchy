package folk.sisby.switchy.api.events;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Describes a "Switch Event" - emitted when a player joins, switches presets, or disconnects.
 *
 * @param player         The relevant player.
 * @param previousPreset The name of the previous preset.
 *                       Null when joining.
 * @param currentPreset  The name of the new current preset.
 *                       Null when disconnecting.
 * @param enabledModules A list of enabled module names for the presets.
 * @author Ami
 * @since 1.8.2
 */
public record SwitchySwitchEvent(UUID player, @Nullable String currentPreset, @Nullable String previousPreset,
								 List<String> enabledModules) {
	private static final String KEY_PLAYER = "player";
	private static final String KEY_CURRENT_PRESET = "currentName";
	private static final String KEY_PREVIOUS_PRESET = "previousName";
	private static final String KEY_ENABLED_MODULES = "enabledModules";

	/**
	 * Deserialize the event from NBT.
	 *
	 * @param nbt an NBT representation of the event.
	 * @return an event constructed from the NBT.
	 */
	public static SwitchySwitchEvent fromNbt(NbtCompound nbt) {
		return new SwitchySwitchEvent(nbt.getUuid(KEY_PLAYER), nbt.contains(KEY_CURRENT_PRESET, NbtElement.STRING_TYPE) ? nbt.getString(KEY_CURRENT_PRESET) : null, nbt.contains(KEY_PREVIOUS_PRESET, NbtElement.STRING_TYPE) ? nbt.getString(KEY_PREVIOUS_PRESET) : null, nbt.getList(KEY_ENABLED_MODULES, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).toList());
	}

	/**
	 * Serialize the event to NBT.
	 *
	 * @return an NBT representation of the event.
	 */
	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid(KEY_PLAYER, player);
		if (currentPreset != null) nbt.putString(KEY_CURRENT_PRESET, currentPreset);
		NbtList nbtModules = new NbtList();
		nbtModules.addAll(enabledModules.stream().map(NbtString::of).toList());
		nbt.put(KEY_ENABLED_MODULES, nbtModules);
		if (previousPreset != null) nbt.putString(KEY_PREVIOUS_PRESET, previousPreset);
		return nbt;
	}
}

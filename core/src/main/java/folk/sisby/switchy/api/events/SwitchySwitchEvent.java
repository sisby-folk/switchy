package folk.sisby.switchy.api.events;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * @author Ami
 * @since 1.8.2
 * Describes a "Switch Event" - emitted when a player joins, switches presets, or disconnects
 */
public class SwitchySwitchEvent {
	/**
	 * The relevant player.
	 */
	public final UUID player;
	/**
	 * The name of the previous preset in the switch.
	 * On player join, this will be null.
	 */
	public final @Nullable String previousPreset; // Null previous preset means "joined"
	/**
	 * The name of the new current preset in the switch.
	 * On player disconnect, this wil be null.
	 */
	public final @Nullable String currentPreset; // Null current preset means "disconnected"
	/**
	 * A list of enabled module names for the presets.
	 */
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

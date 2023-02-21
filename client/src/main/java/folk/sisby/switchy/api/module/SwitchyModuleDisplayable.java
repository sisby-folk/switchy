package folk.sisby.switchy.api.module;

import net.minecraft.nbt.NbtCompound;

public interface SwitchyModuleDisplayable {
	/**
	 * Any data you need to transform using server mods before serializing - do it here.
	 */
	NbtCompound toDisplayNbt();
}

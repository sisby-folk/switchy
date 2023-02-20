package folk.sisby.switchy.api.module;

import net.minecraft.nbt.NbtCompound;

public interface SwitchyModuleData {
	NbtCompound toNbt();

	void fillFromNbt(NbtCompound nbt);
}

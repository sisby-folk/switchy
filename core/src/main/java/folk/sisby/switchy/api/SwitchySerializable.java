package folk.sisby.switchy.api;

import net.minecraft.nbt.NbtCompound;

public interface SwitchySerializable {
	NbtCompound toNbt();

	void fillFromNbt(NbtCompound nbt);
}

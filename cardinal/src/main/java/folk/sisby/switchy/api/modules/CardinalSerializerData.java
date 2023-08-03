package folk.sisby.switchy.api.modules;

import folk.sisby.switchy.api.SwitchySerializable;
import net.minecraft.nbt.NbtCompound;

public class CardinalSerializerData implements SwitchySerializable {
	// Module Data
	protected NbtCompound moduleNbt = new NbtCompound();

	@Override
	public NbtCompound toNbt() {
		return moduleNbt.copy();
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		moduleNbt.copyFrom(nbt);
	}
}

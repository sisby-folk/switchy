package folk.sisby.switchy.api.module;

import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import net.minecraft.nbt.NbtCompound;

/**
 * An extension for {@link SwitchyModule}s that is able to serialize into a format understood by a matching {@link SwitchyClientModule}.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @see SwitchyClientModule
 * @since 2.0.0
 */
public interface SwitchyModuleTransferable extends SwitchySerializable {
	/**
	 * @return an NBT compound legible to {@link SwitchyClientModule#fillFromNbt(NbtCompound)} on the client side.
	 * Runs on the server, must convert data into a format usable for any client with the right {@link SwitchyClientModule}.
	 */
	default NbtCompound toClientNbt() {
		return toNbt();
	}
}

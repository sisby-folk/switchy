package folk.sisby.switchy.api.module;

import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import net.minecraft.nbt.NbtCompound;

/**
 * @author Sisby folk
 * @see SwitchyModule
 * @see SwitchyClientModule
 * An extension for {@link SwitchyModule}s that is able to serialize into a format understood by a matching {@link SwitchyClientModule}.
 * @since 2.0.0
 */
public interface SwitchyModuleClientable {
	/**
	 * @return an NBT compound legible to {@link SwitchyClientModule#fillFromNbt(NbtCompound)} on the client side.
	 * Runs on the server, must convert data into a format usable for any client with the right {@link SwitchyClientModule}.
	 */
	NbtCompound toClientNbt();
}

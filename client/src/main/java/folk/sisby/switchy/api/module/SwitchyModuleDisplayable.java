package folk.sisby.switchy.api.module;

import net.minecraft.nbt.NbtCompound;

/**
 * @author Sisby folk
 * @see SwitchyModule
 * @see folk.sisby.switchy.client.api.module.SwitchyDisplayModule
 * An extension for {@link SwitchyModule}s that is able to serialize into a format understood by a matching {@link folk.sisby.switchy.client.api.module.SwitchyDisplayModule}.
 * @since 2.0.0
 */
public interface SwitchyModuleDisplayable {
	/**
	 * @return an NBT compound legible to {@link folk.sisby.switchy.client.api.module.SwitchyDisplayModule#fillFromNbt(NbtCompound)} on the client side.
	 * Runs on the server, must convert data into a format usable for any client with the right {@link folk.sisby.switchy.client.api.module.SwitchyDisplayModule}.
	 */
	NbtCompound toDisplayNbt();
}

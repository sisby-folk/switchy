package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Request serialized presets for exporting.
 */
public record C2SExportPresets(int listener, NbtCompound presetsNbt) implements CustomPayload {
	public static Id<C2SExportPresets> ID = new Id<>(Identifier.of(Switchy.ID, "c2s_presets_export"));
	public static PacketCodec<PacketByteBuf, C2SExportPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SExportPresets::listener, PacketCodecs.NBT_COMPOUND, C2SExportPresets::presetsNbt, C2SExportPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

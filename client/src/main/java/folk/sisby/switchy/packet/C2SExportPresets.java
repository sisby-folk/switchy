package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Request serialized presets for exporting.
 */
public record C2SExportPresets(int listener, NbtCompound presetsNbt) implements CustomPayload {
	public static Id<C2SExportPresets> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_presets_export"));
	public static PacketCodec<PacketByteBuf, C2SExportPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SExportPresets::listener, PacketCodecs.NBT_COMPOUND, C2SExportPresets::presetsNbt, C2SExportPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

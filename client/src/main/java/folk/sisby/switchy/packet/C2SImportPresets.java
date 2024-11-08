package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Send serialized presets to import.
 * Client: Must be sent twice to finalize - outputs confirmation text in chat.
 */
public record C2SImportPresets(int listener, boolean confirm, NbtCompound presetsNbt) implements CustomPayload {
	public static Id<C2SImportPresets> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_presets_import"));
	public static PacketCodec<PacketByteBuf, C2SImportPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SImportPresets::listener, PacketCodecs.BOOL, C2SImportPresets::confirm, PacketCodecs.NBT_COMPOUND, C2SImportPresets::presetsNbt, C2SImportPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

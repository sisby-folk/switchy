package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Contains the full presets data for exporting
 */
public record S2CExportPresets(int listener, NbtCompound feedbackNbt, NbtCompound presetsNbt) implements CustomPayload {
	public static Id<S2CExportPresets> ID = new Id<>(Feedback.identifier(Switchy.ID, "s2c_presets_export"));
	public static PacketCodec<PacketByteBuf, S2CExportPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, S2CExportPresets::listener, PacketCodecs.NBT_COMPOUND, S2CExportPresets::feedbackNbt, PacketCodecs.NBT_COMPOUND, S2CExportPresets::presetsNbt, S2CExportPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

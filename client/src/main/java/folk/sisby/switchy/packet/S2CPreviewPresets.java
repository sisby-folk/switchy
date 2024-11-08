package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Contains preview-appropriate presets data for viewing
 */
public record S2CPreviewPresets(int listener, NbtCompound feedbackNbt, NbtCompound presetsNbt) implements CustomPayload {
	public static Id<S2CPreviewPresets> ID = new Id<>(Feedback.identifier(Switchy.ID, "s2c_presets_preview"));
	public static PacketCodec<PacketByteBuf, S2CPreviewPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, S2CPreviewPresets::listener, PacketCodecs.NBT_COMPOUND, S2CPreviewPresets::feedbackNbt, PacketCodecs.NBT_COMPOUND, S2CPreviewPresets::presetsNbt, S2CPreviewPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

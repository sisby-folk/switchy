package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Request serialized presets for previewing.
 */
public record C2SPreviewPresets(int listener) implements CustomPayload {
	public static Id<C2SPreviewPresets> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_presets_preview"));
	public static PacketCodec<PacketByteBuf, C2SPreviewPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SPreviewPresets::listener, C2SPreviewPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

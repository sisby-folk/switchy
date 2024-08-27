package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Request serialized presets for previewing.
 */
public record C2SPreviewPresets(int listener) implements CustomPayload {
	public static Id<C2SPreviewPresets> ID = new Id<>(Identifier.of(Switchy.ID, "c2s_presets_preview"));
	public static PacketCodec<PacketByteBuf, C2SPreviewPresets> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SPreviewPresets::listener, C2SPreviewPresets::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

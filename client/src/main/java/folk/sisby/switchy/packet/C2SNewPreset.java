package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Create a new preset
 */
public record C2SNewPreset(int listener, String name) implements CustomPayload {
	public static Id<C2SNewPreset> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_presets_new"));
	public static PacketCodec<PacketByteBuf, C2SNewPreset> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SNewPreset::listener, PacketCodecs.STRING, C2SNewPreset::name, C2SNewPreset::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

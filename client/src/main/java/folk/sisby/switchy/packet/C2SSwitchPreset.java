package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Switch to a specified preset.
 */
public record C2SSwitchPreset(int listener, String name) implements CustomPayload {
	public static Id<C2SSwitchPreset> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_presets_switch"));
	public static PacketCodec<PacketByteBuf, C2SSwitchPreset> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SSwitchPreset::listener, PacketCodecs.STRING, C2SSwitchPreset::name, C2SSwitchPreset::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

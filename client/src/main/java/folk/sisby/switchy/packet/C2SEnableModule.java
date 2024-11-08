package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Enable a specified module.
 */
public record C2SEnableModule(int listener, Identifier id) implements CustomPayload {
	public static Id<C2SEnableModule> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_modules_enable"));
	public static PacketCodec<PacketByteBuf, C2SEnableModule> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SEnableModule::listener, Identifier.PACKET_CODEC, C2SEnableModule::id, C2SEnableModule::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Disable a specified module.
 */
public record C2SDisableModule(int listener, Identifier id) implements CustomPayload {
	public static Id<C2SDisableModule> ID = new Id<>(Feedback.identifier(Switchy.ID, "c2s_modules_disable"));
	public static PacketCodec<PacketByteBuf, C2SDisableModule> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SDisableModule::listener, Identifier.PACKET_CODEC, C2SDisableModule::id, C2SDisableModule::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

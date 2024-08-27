package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Delete the specified preset
 */
public record C2SDeletePreset(int listener, String name) implements CustomPayload {
	public static Id<C2SDeletePreset> ID = new Id<>(Identifier.of(Switchy.ID, "c2s_presets_delete"));
	public static PacketCodec<PacketByteBuf, C2SDeletePreset> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SDeletePreset::listener, PacketCodecs.STRING, C2SDeletePreset::name, C2SDeletePreset::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

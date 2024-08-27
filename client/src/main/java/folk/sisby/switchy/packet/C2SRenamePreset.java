package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Rename the specified preset
 */
public record C2SRenamePreset(int listener, String oldName, String newName) implements CustomPayload {
	public static Id<C2SRenamePreset> ID = new Id<>(Identifier.of(Switchy.ID, "c2s_presets_rename"));
	public static PacketCodec<PacketByteBuf, C2SRenamePreset> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, C2SRenamePreset::listener, PacketCodecs.STRING, C2SRenamePreset::oldName, PacketCodecs.STRING, C2SRenamePreset::newName, C2SRenamePreset::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

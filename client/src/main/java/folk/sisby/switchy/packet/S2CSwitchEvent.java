package folk.sisby.switchy.packet;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.util.Feedback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Replicates a switch event to the client
 */
public record S2CSwitchEvent(NbtCompound eventNbt) implements CustomPayload {
	public static Id<S2CSwitchEvent> ID = new Id<>(Feedback.identifier(Switchy.ID, "s2c_events_switch"));
	public static PacketCodec<PacketByteBuf, S2CSwitchEvent> CODEC = PacketCodec.tuple(PacketCodecs.NBT_COMPOUND, S2CSwitchEvent::eventNbt, S2CSwitchEvent::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

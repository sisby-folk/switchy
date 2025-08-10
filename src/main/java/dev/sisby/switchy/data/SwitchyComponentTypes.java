package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

	public static final SwitchyComponentType<Text> NAME = instance().register(Switchy.id("name"), id -> SwitchyComponentType.builder(id, TextCodecs.CODEC)
		.packetCodec(TextCodecs.PACKET_CODEC).build());
	public static final SwitchyComponentType<Float> HEALTH = instance().register(Switchy.id("health"), id -> SwitchyComponentType.builder(id, Codec.FLOAT)
		.nbtSwitcher("Health").packetCodec(PacketCodecs.FLOAT).build());
	public static final SwitchyComponentType<Vec3d> POS = instance().register(Switchy.id("pos"), id -> SwitchyComponentType.builder(id, Vec3d.CODEC)
		.nbtSwitcher("Pos").build());
	public static final SwitchyComponentType<Identifier> DIMENSION = instance().register(Switchy.id("dimension"), id -> SwitchyComponentType.builder(id, Identifier.CODEC)
		.nbtSwitcher("Dimension").packetCodec(Identifier.PACKET_CODEC).build());

	public static void init() {
		// static init
	}

	public static SwitchyComponentTypes instance() {
		return INSTANCE;
	}
}

package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

	public static final SwitchyComponentType<Text> NAME = instance().register(Switchy.id("name"), id -> SwitchyComponentType.<Text>builder(id)
		.codec(TextCodecs.CODEC).packetCodec(TextCodecs.PACKET_CODEC).build());
	public static final SwitchyComponentType<Float> HEALTH = instance().register(Switchy.id("health"), id -> SwitchyComponentType.<Float>builder(id)
		.codec(Codec.FLOAT).packetCodec(PacketCodecs.FLOAT).build());

	public static void init() {
	}

	public static SwitchyComponentTypes instance() {
		return INSTANCE;
	}
}

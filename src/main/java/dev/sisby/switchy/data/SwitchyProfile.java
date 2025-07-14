package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record SwitchyProfile(String id, SwitchyComponentMap components) implements SwitchyComponentHolder<SwitchyProfile> {
	public static Codec<SwitchyProfile> codec(String id) {
		return RecordCodecBuilder.create(instance -> instance.group(
			SwitchyComponentMap.CODEC.fieldOf("components").forGetter(SwitchyProfile::components)
		).apply(instance, components -> new SwitchyProfile(id, components)));
	}

	public static PacketCodec<RegistryByteBuf, SwitchyProfile> packetCodec(String id) {
		return PacketCodec.tuple(
			SwitchyComponentMap.PACKET_CODEC, SwitchyProfile::components,
			components -> new SwitchyProfile(id, components)
		);
	}
}

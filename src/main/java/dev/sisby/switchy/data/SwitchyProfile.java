package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record SwitchyProfile(SwitchyComponentMap components) implements SwitchyComponentHolder {
	public static final Codec<SwitchyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		SwitchyComponentMap.CODEC.fieldOf("components").forGetter(SwitchyProfile::components)
	).apply(instance, SwitchyProfile::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyProfile> PACKET_CODEC = PacketCodec.tuple(
		SwitchyComponentMap.PACKET_CODEC, SwitchyProfile::components,
		SwitchyProfile::new
	);
}

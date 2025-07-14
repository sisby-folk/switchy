package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.util.SwitchyCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record SwitchyLayer(String current, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
	public static final Codec<SwitchyLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("current").forGetter(SwitchyLayer::current),
		SwitchyCodecs.COMPONENT_TYPE_SET_CODEC.fieldOf("componentTypes").forGetter(SwitchyLayer::componentTypes),
		Codec.unboundedMap(Codec.STRING, SwitchyProfile.CODEC).fieldOf("profiles").forGetter(SwitchyLayer::profiles)
	).apply(instance, SwitchyLayer::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyLayer> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SwitchyLayer::current,
		PacketCodecs.collection(LinkedHashSet::new, SwitchyComponentTypes.instance().packetCodec()), SwitchyLayer::componentTypes,
		PacketCodecs.map(HashMap::new, PacketCodecs.STRING, SwitchyProfile.PACKET_CODEC), SwitchyLayer::profiles,
		SwitchyLayer::new
	);
}

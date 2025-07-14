package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.util.SwitchyCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record SwitchyLayer(String current, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
	public static final Codec<SwitchyLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("current").forGetter(SwitchyLayer::current),
		SwitchyCodecs.COMPONENT_TYPE_SET_CODEC.fieldOf("componentTypes").forGetter(SwitchyLayer::componentTypes),
		Codec.dispatchedMap(Codecs.NON_EMPTY_STRING, SwitchyProfile::codec).fieldOf("profiles").forGetter(SwitchyLayer::profiles)
	).apply(instance, SwitchyLayer::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyLayer> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SwitchyLayer::current,
		PacketCodecs.collection(LinkedHashSet::new, SwitchyComponentTypes.instance().packetCodec()), SwitchyLayer::componentTypes,
		SwitchyCodecs.packetDispatchedMap(HashMap::new, PacketCodecs.STRING, SwitchyProfile::packetCodec), SwitchyLayer::profiles,
		SwitchyLayer::new
	);
}

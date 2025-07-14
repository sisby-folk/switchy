package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public record SwitchyPlayerData(Map<SwitchyLayerType, SwitchyLayer> layers) {
	public static final String KEY = "player_data";
	public static final Codec<SwitchyPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.unboundedMap(SwitchyLayerTypes.instance().codec(), SwitchyLayer.CODEC).fieldOf("layers").forGetter(SwitchyPlayerData::layers)
	).apply(instance, SwitchyPlayerData::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyPlayerData> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.map(HashMap::new, SwitchyLayerTypes.instance().packetCodec(), SwitchyLayer.PACKET_CODEC), SwitchyPlayerData::layers,
		SwitchyPlayerData::new
	);

	public static SwitchyPlayerData create() {
		Map<SwitchyLayerType, SwitchyLayer> layers = new LinkedHashMap<>();
		for (SwitchyLayerType type : SwitchyLayerTypes.instance().values()) {
			layers.put(type, new SwitchyLayer(
				"default", // Represents the profile actively in player data (which does not exist!! in profiles)
				new LinkedHashSet<>(SwitchyComponentTypes.instance().values().stream().filter(t -> t.defaultLayerType() == type).toList()),
				new LinkedHashMap<>()
			));
		}
		return new SwitchyPlayerData(layers);
	}
}

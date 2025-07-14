package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
				new LinkedHashMap<>(Map.of("default", new SwitchyProfile("default", SwitchyComponentMap.builder().add(SwitchyComponentTypes.NAME, Text.of("Default")).build())))
			));
		}
		return new SwitchyPlayerData(layers);
	}

	public boolean profileExists(SwitchyLayerType layerType, String profileId) {
		return layers.get(layerType).profiles().containsKey(profileId);
	}

	public SwitchyProfile getCurrentProfile(SwitchyLayerType layer) {
		return layers.get(layer).profiles().get(layers.get(layer).current());
	}

	public SwitchyProfile createProfile(SwitchyLayerType layer, String profileId, String profileName, ServerPlayerEntity player) {
		return layers.get(layer).profiles().put(profileId, new SwitchyProfile(profileId, SwitchyComponentMap.builder().add(SwitchyComponentTypes.NAME, Text.of(profileName)).build()));
	}

	public SwitchyProfile switchOrCreateProfile(SwitchyLayerType layerType, String profileId, ServerPlayerEntity player) {
		if (!profileExists(layerType, profileId)) createProfile(layerType, profileId, profileId.toUpperCase(), player);
		layers.put(layerType, new SwitchyLayer(profileId, layers.get(layerType).componentTypes(), layers.get(layerType).profiles()));
		return getCurrentProfile(layerType);
	}
}

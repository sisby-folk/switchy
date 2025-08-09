package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.util.SwitchyCodecs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record SwitchyPlayerData(String current, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
	public static final String KEY = "player_data";
	public static final Codec<SwitchyPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("current").forGetter(SwitchyPlayerData::current),
		SwitchyCodecs.COMPONENT_TYPE_SET_CODEC.fieldOf("componentTypes").forGetter(SwitchyPlayerData::componentTypes),
		Codec.dispatchedMap(Codecs.NON_EMPTY_STRING, SwitchyProfile::codec).fieldOf("profiles").xmap(a -> (Map<String, SwitchyProfile>) new HashMap<>(a), b -> b).forGetter(SwitchyPlayerData::profiles)
	).apply(instance, SwitchyPlayerData::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyPlayerData> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SwitchyPlayerData::current,
		PacketCodecs.collection(LinkedHashSet::new, SwitchyComponentTypes.instance().packetCodec()), SwitchyPlayerData::componentTypes,
		SwitchyCodecs.packetDispatchedMap(HashMap::new, PacketCodecs.STRING, SwitchyProfile::packetCodec), SwitchyPlayerData::profiles,
		SwitchyPlayerData::new
	);

	public static SwitchyPlayerData create() {
		return new SwitchyPlayerData(
			"default",
			new LinkedHashSet<>(SwitchyComponentTypes.instance().values()),
			new LinkedHashMap<>(Map.of("default", new SwitchyProfile("default", SwitchyComponentMap.builder().add(SwitchyComponentTypes.NAME, Text.of("Default")).build())))
		);
	}

	public boolean profileExists(String profileId) {
		return profiles().containsKey(profileId);
	}

	public SwitchyProfile getCurrentProfile() {
		return profiles().get(current());
	}

	public SwitchyProfile createProfile(String profileId, String profileName, ServerPlayerEntity player) throws Exception {
		NbtCompound playerNbt = new NbtCompound();
		player.writeNbt(playerNbt);
		SwitchyComponentMap.Builder builder = SwitchyComponentMap.builder();
		builder.add(SwitchyComponentTypes.NAME, Text.of(profileName));
		for (SwitchyComponentType<?> componentType : SwitchyComponentTypes.instance().values()) {
			componentType.tryInitialize(builder, new SwitchyComponentType.Initializer.InitializerContext(playerNbt, player));
		}
		return profiles().put(profileId, new SwitchyProfile(profileId, builder.build()));
	}

	public SwitchyProfile switchOrCreateProfile(String profileId, ServerPlayerEntity player) throws Exception {
		if (!profileExists(profileId)) createProfile(profileId, profileId.toUpperCase(), player);
		return getCurrentProfile();
	}
}

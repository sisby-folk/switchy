package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.duck.SwitchyPlayer;
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

public class SwitchyPlayerData {
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

	private String current;
	private final Set<SwitchyComponentType<?>> componentTypes;
	private final Map<String, SwitchyProfile> profiles;

	public SwitchyPlayerData(String current, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
		this.current = current;
		this.componentTypes = componentTypes;
		this.profiles = profiles;
	}

	public static SwitchyPlayerData create() {
		return new SwitchyPlayerData(
			"default",
			new LinkedHashSet<>(SwitchyComponentTypes.instance().values()),
			new LinkedHashMap<>(Map.of("default", new SwitchyProfile("default", SwitchyComponentMap.builder().add(SwitchyComponentTypes.NAME, Text.of("DEFAULT")).build())))
		);
	}

	public boolean profileExists(String profileId) {
		return profiles().containsKey(profileId);
	}

	public SwitchyProfile getCurrentProfile() {
		return profiles().get(current());
	}

	public SwitchyProfile getOrCreateProfile(String profileId, String profileName, ServerPlayerEntity player) throws Exception {
		if (profileExists(profileId)) profiles.get(profileId);
		NbtCompound playerNbt = new NbtCompound();
		player.writeNbt(playerNbt);
		SwitchyComponentMap.Builder builder = SwitchyComponentMap.builder();
		builder.add(SwitchyComponentTypes.NAME, Text.of(profileName));
		for (SwitchyComponentType<?> componentType : componentTypes) {
			componentType.tryInitialize(builder, new SwitchyComponentType.Initializer.InitializerContext(playerNbt, player));
		}
		SwitchyProfile newProfile = new SwitchyProfile(profileId, builder.build());
		profiles().put(profileId, newProfile);
		return newProfile;
	}

	private void switchProfile(SwitchyProfile nextProfile, ServerPlayerEntity player) throws Exception {
		if (nextProfile.id().equals(current)) throw new IllegalAccessException("can't switch to the current profile!");
		SwitchyProfile currentProfile = getCurrentProfile();
		// Read Components
		NbtCompound playerNbt = new NbtCompound();
		player.writeNbt(playerNbt);
		for (SwitchyComponentType<?> componentType : currentProfile.components().keySet()) {
			if (componentType.reader() != null) {
				currentProfile.components().set(componentType, componentType.reader().read(new SwitchyComponentType.Reader.ReaderContext(playerNbt, player)));
			}
		}
		// Mutate NBT
		for (SwitchyComponentType<?> componentType : nextProfile.components().keySet()) {
			componentType.tryMutate(nextProfile.components(), playerNbt);
		}

		current = nextProfile.id();

		((SwitchyPlayer) player).switchy$hotSwap(playerNbt);
	}

	public SwitchyProfile switchOrCreateProfile(String profileId, ServerPlayerEntity player) throws Exception {
		switchProfile(getOrCreateProfile(profileId.toLowerCase(), profileId.toUpperCase(), player), player);
		return getCurrentProfile();
	}

	public String current() {
		return current;
	}

	public Set<SwitchyComponentType<?>> componentTypes() {
		return componentTypes;
	}

	public Map<String, SwitchyProfile> profiles() {
		return profiles;
	}
}

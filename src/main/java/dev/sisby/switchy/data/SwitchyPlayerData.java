package dev.sisby.switchy.data;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.util.SwitchyCodecs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SwitchyPlayerData {
	public static final Codec<SwitchyPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("current").forGetter(SwitchyPlayerData::current),
		SwitchyCodecs.COMPONENT_TYPE_SET_CODEC.fieldOf("componentTypes").forGetter(p -> p.componentTypes),
		Codec.dispatchedMap(Codecs.NON_EMPTY_STRING, SwitchyProfile::codec).fieldOf("profiles").xmap(a -> (Map<String, SwitchyProfile>) new HashMap<>(a), b -> b).forGetter(p -> p.profiles)
	).apply(instance, SwitchyPlayerData::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyPlayerData> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SwitchyPlayerData::current,
		PacketCodecs.collection(LinkedHashSet::new, SwitchyComponentTypes.instance().packetCodec()), p -> p.componentTypes,
		SwitchyCodecs.packetDispatchedMap(HashMap::new, PacketCodecs.STRING, SwitchyProfile::packetCodec), p -> p.profiles,
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

	public static SwitchyPlayerData of(ServerPlayerEntity player) {
		return ((SwitchyPlayer) player).switchy$playerData();
	}

	public static SwitchyPlayerData create(ServerPlayerEntity player) {
		SwitchyPlayerData data = new SwitchyPlayerData(
			"default",
			new LinkedHashSet<>(SwitchyComponentTypes.instance().values()),
			new LinkedHashMap<>()
		);
		data.getOrCreateProfile("default", "DEFAULT", player);
		return data;
	}

	public boolean profileExists(String profileId) {
		return profiles.containsKey(profileId);
	}

	public SwitchyProfile getCurrentProfile() {
		return profiles.get(current());
	}

	public Set<String> keySet() {
		return profiles.keySet();
	}

	public Collection<SwitchyProfile> values() {
		return profiles.values();
	}

	public int size() {
		return profiles.size();
	}

	public String current() {
		return current;
	}

	public SwitchyProfile getProfile(String profileId) {
		return profiles.get(profileId);
	}

	public void init(ServerPlayerEntity player, NbtCompound nbt) {
		for (SwitchyComponentType<?> componentType : Sets.difference(SwitchyComponentTypes.instance().values(), componentTypes)) {
			if (componentType.nbtReader() != null) {
				for (SwitchyProfile profile : profiles.values()) {
					try {
						componentType.tryInitialize(profile.components(), nbt, player);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				componentTypes.add(componentType);
			}
		}
	}

	public SwitchyProfile getOrCreateProfile(String profileId, String profileName, ServerPlayerEntity player) {
		if (profileExists(profileId)) return profiles.get(profileId);
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		SwitchyComponentMap components = SwitchyComponentMap.empty();
		components.set(SwitchyComponentTypes.NAME, Text.of(profileName));
		for (SwitchyComponentType<?> componentType : componentTypes) {
			try {
				componentType.tryInitialize(components, nbt, player);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		SwitchyProfile newProfile = new SwitchyProfile(profileId, components);
		profiles.put(profileId, newProfile);
		return newProfile;
	}

	private NbtCompound updateFromPlayer(SwitchyProfile profile, ServerPlayerEntity player) throws Exception {
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		for (SwitchyComponentType<?> componentType : componentTypes) {
			if (componentType.nbtReader() != null) {
				profile.components().set(componentType, componentType.nbtReader().read(nbt));
			}
		}
		return nbt;
	}

	public void updateCurrent(ServerPlayerEntity player) throws Exception {
		updateFromPlayer(getCurrentProfile(), player);
	}

	public void renameProfile(String oldId, String newId) throws IllegalArgumentException {
		if (!profileExists(oldId)) throw new IllegalArgumentException("profile doesn't exist!");
		if (profileExists(newId)) throw new IllegalArgumentException("new id is already in use!");
		profiles.put(newId, profiles.remove(oldId).withId(newId));
		if (current.equals(oldId)) current = newId;
	}

	private void switchProfile(SwitchyProfile nextProfile, ServerPlayerEntity player) throws Exception {
		if (nextProfile.id().equals(current)) throw new IllegalArgumentException("can't switch to the current profile!");
		SwitchyProfile currentProfile = getCurrentProfile();
		// Read Components
		NbtCompound playerNbt = updateFromPlayer(currentProfile, player);
		// Mutate NBT
		for (SwitchyComponentType<?> componentType : nextProfile.components().keySet()) {
			componentType.tryMutate(nextProfile.components(), playerNbt);
		}

		current = nextProfile.id();

		((SwitchyPlayer) player).switchy$hotSwap(playerNbt, SwitchyCommands.prefix()
			.append(Text.literal("Switching to ").formatted(Formatting.GRAY))
			.append(nextProfile.getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())))
			.append(Text.literal("! Please reconnect.").formatted(Formatting.GRAY))
		);
	}

	public SwitchyProfile switchOrCreateProfile(String profileId, ServerPlayerEntity player) throws Exception {
		switchProfile(getOrCreateProfile(profileId.toLowerCase(), profileId.toUpperCase(), player), player);
		return getCurrentProfile();
	}
}

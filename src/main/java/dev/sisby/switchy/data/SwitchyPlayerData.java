package dev.sisby.switchy.data;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.exception.ProfileCurrentException;
import dev.sisby.switchy.exception.ProfileMissingException;
import dev.sisby.switchy.exception.ProfilePreciousException;
import dev.sisby.switchy.exception.ProfileExistsException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchyPlayerData {
	public static final Codec<SwitchyPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("current").forGetter(SwitchyPlayerData::current),
		Codec.STRING.fieldOf("previous").forGetter(SwitchyPlayerData::previous),
		SwitchyCodecs.COMPONENT_TYPE_SET_CODEC.fieldOf("componentTypes").forGetter(p -> p.componentTypes),
		Codec.dispatchedMap(Codecs.NON_EMPTY_STRING, SwitchyProfile::codec).fieldOf("profiles").xmap(a -> (Map<String, SwitchyProfile>) new HashMap<>(a), b -> b).forGetter(p -> p.profiles)
	).apply(instance, SwitchyPlayerData::new));

	public static final PacketCodec<RegistryByteBuf, SwitchyPlayerData> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SwitchyPlayerData::current,
		PacketCodecs.STRING, SwitchyPlayerData::previous,
		PacketCodecs.collection(LinkedHashSet::new, SwitchyComponentTypes.instance().packetCodec()), p -> p.componentTypes,
		SwitchyCodecs.packetDispatchedMap(HashMap::new, PacketCodecs.STRING, SwitchyProfile::packetCodec), p -> p.profiles,
		SwitchyPlayerData::new
	);

	private String current;
	private String previous;
	private final Set<SwitchyComponentType<?>> componentTypes;
	private final Map<String, SwitchyProfile> profiles;

	public SwitchyPlayerData(String current, String previous, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
		this.current = current;
		this.previous = previous;
		this.componentTypes = componentTypes;
		this.profiles = profiles;
	}

	public static SwitchyPlayerData of(ServerPlayerEntity player) {
		return ((SwitchyPlayer) player).switchy$playerData();
	}

	public static SwitchyPlayerData create(ServerPlayerEntity player) {
		SwitchyPlayerData data = new SwitchyPlayerData(
			"default",
			"",
			new LinkedHashSet<>(SwitchyComponentTypes.instance().values()),
			new LinkedHashMap<>()
		);
		data.profiles.put("default", new SwitchyProfile("default", SwitchyComponentMap.empty()));
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

	public String previous() {
		return previous;
	}

	public SwitchyProfile getProfile(String profileId) {
		return profiles.get(profileId);
	}

	public void init(ServerPlayerEntity player, NbtCompound nbt) {
		for (SwitchyComponentType<?> componentType : Sets.difference(SwitchyComponentTypes.instance().values(), componentTypes)) {
			try {
				componentType.tryInitialize(profiles.values().stream().map(SwitchyProfile::components).toList(), nbt, player);
			} catch (Exception e) {
				Switchy.LOGGER.warn("Failed to initialize {} for {}", componentType.id(), player.getGameProfile().getName(), e);
				continue;
			}
			componentTypes.add(componentType);
		}
	}

	public SwitchyProfile getOrCreateProfile(String profileId, ServerPlayerEntity player) {
		if (profileExists(profileId)) return profiles.get(profileId);
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		SwitchyComponentMap components = SwitchyComponentMap.empty();
		for (SwitchyComponentType<?> componentType : componentTypes) {
			try {
				componentType.tryInitialize(List.of(components), nbt, player);
			} catch (Exception e) {
				Switchy.LOGGER.warn("Failed to initialize {} for {} profile {}", componentType.id(), player.getGameProfile().getName(), profileId, e);
			}
		}
		SwitchyProfile newProfile = new SwitchyProfile(profileId, components);
		profiles.put(profileId, newProfile);
		return newProfile;
	}

	private NbtCompound updateFromPlayer(SwitchyProfile profile, ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		for (SwitchyComponentType<?> componentType : componentTypes) {
			if (componentType.nbtReader() != null) {
				profile.components().set(componentType, componentType.nbtReader().read(nbt));
			}
		}
		return nbt;
	}

	public void updateCurrent(ServerPlayerEntity player) {
		updateFromPlayer(getCurrentProfile(), player);
	}

	public void renameProfile(String oldId, String newId) throws IllegalArgumentException {
		if (!profileExists(oldId)) throw new ProfileMissingException(oldId);
		if (profileExists(newId)) throw new ProfileExistsException(newId);
		profiles.put(newId, profiles.remove(oldId).withId(newId));
		if (current.equals(oldId)) current = newId;
	}

	public SwitchyProfile deleteProfile(String profileId) {
		if (current.equals(profileId)) throw new ProfileCurrentException(profileId);
		if (!profileExists(profileId)) throw new ProfileMissingException(profileId);
		SwitchyProfile profile = getProfile(profileId);
		var preciousComponents = profile.components().keySet().stream().filter(t -> t.isPrecious(profile.components())).collect(Collectors.toSet());
		if (!preciousComponents.isEmpty()) throw new ProfilePreciousException(preciousComponents, profile.components());
		profiles.remove(profileId);
		return profile;
	}

	private void switchProfile(SwitchyProfile nextProfile, ServerPlayerEntity player) {
		if (nextProfile.id().equals(current)) throw new ProfileCurrentException(nextProfile.id());
		SwitchyProfile currentProfile = getCurrentProfile();
		// Read Components
		NbtCompound playerNbt = updateFromPlayer(currentProfile, player);
		// Mutate NBT
		for (SwitchyComponentType<?> componentType : nextProfile.components().keySet()) {
			componentType.tryMutate(nextProfile.components(), playerNbt);
		}

		previous = current;
		current = nextProfile.id();

		((SwitchyPlayer) player).switchy$hotSwap(playerNbt, SwitchyCommands.prefix()
			.append(Text.literal("Switching to ").formatted(Formatting.GRAY))
			.append(nextProfile.getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())))
			.append(Text.literal("! Please reconnect.").formatted(Formatting.GRAY))
		);
	}

	public SwitchyProfile switchOrCreateProfile(String profileId, ServerPlayerEntity player) {
		switchProfile(getOrCreateProfile(profileId.toLowerCase(), player), player);
		return getCurrentProfile();
	}

	public void clearPrevious() {
		previous = "";
	}
}

package dev.sisby.switchy.data;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.exception.NbtException;
import dev.sisby.switchy.exception.ProfileCurrentException;
import dev.sisby.switchy.exception.ProfileMissingException;
import dev.sisby.switchy.exception.ProfilePreciousException;
import dev.sisby.switchy.exception.ProfileExistsException;
import dev.sisby.switchy.mixin.AccessServerPlayer;
import dev.sisby.switchy.util.DispatchMapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SwitchyPlayerData {
	private static Codec<SwitchyPlayerData> codec(SwitchyComponentTypes types) {
		return RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("current").forGetter(SwitchyPlayerData::current),
			ComponentSerialization.CODEC.optionalFieldOf("greeting").forGetter(SwitchyPlayerData::greeting),
			types.SET_CODEC.fieldOf("componentTypes").forGetter(p -> p.componentTypes),
			DispatchMapCodec.of(ExtraCodecs.NON_EMPTY_STRING, id -> SwitchyProfile.codec(types, id)).fieldOf("profiles").xmap(a -> (Map<String, SwitchyProfile>) new HashMap<>(a), b -> b).forGetter(p -> p.profiles)
		).apply(instance, (current, optionalGreeting, componentTypes, profiles) -> new SwitchyPlayerData(current, optionalGreeting.orElse(null), componentTypes, profiles)));
	}

	private String current;
	private Component greeting;
	private final Set<SwitchyComponentType<?>> componentTypes;
	private final Map<String, SwitchyProfile> profiles;

	public SwitchyPlayerData(String current, Component greeting, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
		this.current = current;
		this.greeting = greeting;
		this.componentTypes = componentTypes;
		this.profiles = profiles;
	}

	public static SwitchyPlayerData of(ServerPlayer player) {
		return ((SwitchyPlayer) player).switchy$getOrCreatePlayerData();
	}

	public static SwitchyPlayerData ofEarly(ServerPlayer player) {
		return ((SwitchyPlayer) player).switchy$getPlayerData();
	}

	public static SwitchyPlayerData create(ServerPlayer player, ValueInput input) {
		SwitchyPlayerData data = new SwitchyPlayerData(
			"default",
			null,
			new LinkedHashSet<>(),
			new LinkedHashMap<>()
		);
		data.profiles.put("default", new SwitchyProfile("default", SwitchyComponentMap.empty()));
		for (SwitchyComponentType<?> componentType : Sets.difference(SwitchyComponentTypes.instance().values(), data.componentTypes)) {
			data.initComponent(componentType, player, input);
		}
		return data;
	}

	public boolean profileExists(String profileId) {
		return profiles.containsKey(profileId);
	}

	public SwitchyProfile getCurrentProfile(ServerPlayer player) throws NbtException {
		return getProfile(current(), player);
	}

	public Set<String> keySet() {
		return profiles.keySet();
	}

	public Set<SwitchyComponentType<?>> componentSet() {
		return componentTypes;
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

	public Optional<Component> greeting() {
		return Optional.ofNullable(greeting);
	}

	public Component greet(ServerPlayer player) {
		Component defaultedGreeting = Optional.ofNullable(greeting).orElseGet(() -> SwitchyCommands.prefix()
			.append(Component.literal("welcome back! current profile: ").withStyle(ChatFormatting.GRAY))
			.append(SwitchyCommands.getProfileText(player, profiles.get(current)))
			.append(Component.literal(". ").withStyle(ChatFormatting.GRAY))
			.append(SwitchyCommands.clickable("list", "/switchy", true)));
		greeting = null;
		return defaultedGreeting;
	}

	public SwitchyProfile getProfile(String profileId, ServerPlayer player) throws NbtException {
		if (profileId.equals(current)) updateFromPlayer(profiles.get(profileId), player);
		return profiles.get(profileId);
	}

	public int initComponents(Set<SwitchyComponentType<?>> types, ServerPlayer player) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess());
		player.saveWithoutId(output);
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess(), output.buildResult());
		for (SwitchyComponentType<?> type : types) {
			if (initComponent(type, player, input)) {
				types.add(type);
			} else { // roll back
				types.forEach(this::removeComponent);
				return 0;
			}
		}
		return types.size();
	}

	public boolean initComponent(SwitchyComponentType<?> componentType, ServerPlayer player, ValueInput nbt) {
		try {
			componentType.tryInitialize(profiles.values().stream().map(SwitchyProfile::components).toList(), nbt, player, player.getGameProfile().name());
		} catch (Exception e) {
			Switchy.LOGGER.warn("Failed to initialize {} for {}", componentType.id(), player.getGameProfile().name(), e);
			return false;
		}
		componentTypes.add(componentType);
		return true;
	}

	public int removeComponents(Set<SwitchyComponentType<?>> types) {
		if (profiles.values().stream().anyMatch(p -> !p.id().equals(current) && types.stream().anyMatch(t -> t.isPrecious(p.components())))) return 0;
		for (SwitchyComponentType<?> type : types) {
			for (SwitchyProfile p : profiles.values()) {
				p.remove(type);
			}
			componentTypes.remove(type);
		}
		return types.size();
	}

	public boolean removeComponent(SwitchyComponentType<?> componentType) {
		if (!profiles.values().stream().filter(p -> !p.id().equals(current) && componentType.isPrecious(p.components())).toList().isEmpty()) return false;
		for (SwitchyProfile profile : profiles.values()) {
			profile.remove(componentType);
		}
		componentTypes.remove(componentType);
		return true;
	}

	public SwitchyProfile getOrCreateProfile(String profileId, ServerPlayer player) {
		if (profileExists(profileId)) return profiles.get(profileId);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess());
		player.saveWithoutId(output);
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess(), output.buildResult());
		SwitchyComponentMap components = SwitchyComponentMap.empty();
		for (SwitchyComponentType<?> componentType : componentTypes) {
			try {
				componentType.tryInitialize(List.of(components), input, player, profileId);
			} catch (Exception e) {
				Switchy.LOGGER.warn("Failed to initialize {} for {} profile {}", componentType.id(), player.getGameProfile().name(), profileId, e);
			}
		}
		SwitchyProfile newProfile = new SwitchyProfile(profileId, components);
		profiles.put(profileId, newProfile);
		return newProfile;
	}

	public void validate(ServerPlayer self, ValueInput nbt) {
		Set<Identifier> groupsChecked = new HashSet<>();
		for (SwitchyComponentType<?> type : new HashSet<>(componentTypes)) {
			Identifier group = type.group();
			if (group != null && !groupsChecked.contains(group)) {
				groupsChecked.add(group);
				for (SwitchyComponentType<?> otherType : SwitchyComponentTypes.instance().values()) {
					if (group.equals(otherType.group()) && !componentTypes.contains(otherType)) {
						Switchy.LOGGER.info("[Switchy] Enabling component {} of partially enabled group {} for user {}", otherType.id(), group, self.getGameProfile().name());
						initComponent(otherType, self, nbt);
					}
				}
			}
		}
	}

	public String profileAfter(String current) {
		if (!profileExists(current)) throw new ProfileMissingException(current);
		List<String> orderedProfiles = profiles.keySet().stream().sorted().toList();
		return orderedProfiles.get((orderedProfiles.indexOf(current) + 1) % orderedProfiles.size());
	}

	public String randomBesides(String current, RandomSource random) {
		if (!profileExists(current)) throw new ProfileMissingException(current);
		List<String> orderedProfiles = new ArrayList<>(profiles.keySet().stream().sorted().toList());
		orderedProfiles.remove(current);
		return orderedProfiles.get(random.nextInt(orderedProfiles.size()));
	}

	public record ProxyTag(@Nullable String prefix, @Nullable String suffix) {}

	public record ProfileImportData(@Nullable String id, String name, @Nullable String display_name, @Nullable String color, @Nullable String pronouns, @Nullable String description, @Nullable String avatar_url, @Nullable List<ProxyTag> proxy_tags, @Nullable Map<String, JsonElement> components) {}

	public record GroupImportData(String name, List<String> members) {}

	private static final Pattern PARENTHESES = Pattern.compile("\\S.*([<(\\[][^>)\\]]*[)>\\]])");

	private static String quickTextEscape(String input) {
		return input.replace("<", "\\<").replace("'", "\\'");
	}

	public int importProfiles(List<ProfileImportData> profileData, ServerPlayer player, @Nullable String name, boolean allowNew, Function<Integer, Component> greetingGetter) throws NbtException {
		int updated = 0;
		boolean hadOneProfile = size() == 1;
		SwitchyProfile newCurrent = null;
		for (ProfileImportData data : profileData) {
			String id = data.name().toLowerCase();
			if (!allowNew && !keySet().contains(id)) continue;
			updated++;
			SwitchyProfile profile = getOrCreateProfile(id, player);
			StringBuilder bracketed = new StringBuilder();
			if (data.display_name() != null) { // discord is better with long names. let's put it in the bio instead
				Matcher matcher = PARENTHESES.matcher(data.display_name());
				while (matcher.find()) {
					bracketed.append(matcher.group(1));
				}
			}
			String newName = "<hover:'%s%s | %s%s'><#%s>%s".formatted(
				bracketed.isEmpty() ? "" : quickTextEscape(bracketed.toString()) + (data.pronouns() != null ? " - " : ""),
				quickTextEscape(Objects.requireNonNullElse(data.pronouns(), "")),
				quickTextEscape(Objects.requireNonNullElse(name, player.getGameProfile().name())),
				quickTextEscape(data.description() == null ? "" : " | " + data.description()),
				quickTextEscape(Objects.requireNonNullElse(data.color(), "FFFFFF")),
				quickTextEscape(Objects.requireNonNullElse(data.display_name(), id).replace(bracketed, "")).trim());
			if (componentSet().contains(SwitchyComponentTypes.NAME) && !newName.equals(profile.get(SwitchyComponentTypes.NAME))) {
				if (current.equals(id)) newCurrent = profile;
				profile.set(SwitchyComponentTypes.NAME, newName);
			}
			if (componentSet().contains(SwitchyComponentTypes.TAG) && data.proxy_tags() != null && !data.proxy_tags().isEmpty()) {
				profile.set(SwitchyComponentTypes.TAG, data.proxy_tags().stream().map(t -> new SwitchyComponentTypes.Tag(Objects.requireNonNullElse(t.prefix(), ""), Objects.requireNonNullElse(t.suffix(), ""))).toList());
			}
			if (data.components() != null) {
				for (String componentKey : data.components().keySet()) {
					SwitchyComponentType<?> type = componentSet().stream().filter(t -> t.id().toString().equals(componentKey)).findFirst().orElse(null);
					if (type != null && type.importable()) {
						if (current.equals(id)) newCurrent = profile;
						type.decode(((AccessServerPlayer) player).getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE), data.components.get(componentKey), profile.components());
					}
				}
			}
		}
		if (newCurrent != null || (hadOneProfile && size() > 1)) {
			if (newCurrent == null) newCurrent = getCurrentProfile(player);
			selfSwitch(newCurrent, player, greetingGetter.apply(updated));
		}
		return updated;
	}

	private CompoundTag updateFromPlayer(SwitchyProfile profile, ServerPlayer player) throws NbtException {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess());
		player.saveWithoutId(output);
		CompoundTag nbt = output.buildResult();
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess(), nbt);
		for (SwitchyComponentType<?> componentType : componentTypes) {
			if (componentType.nbtReader() != null) {
				profile.components().set(componentType, componentType.nbtReader().read(((AccessServerPlayer) player).getServer().registryAccess(), input));
			} else if (componentType.playerReader() != null) {
				profile.components().set(componentType, componentType.playerReader().read(player, profile.id()));
			}
		}
		return nbt;
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
		SwitchyProfile profile = profiles.get(profileId);
		var preciousComponents = profile.components().keySet().stream().filter(t -> t.isPrecious(profile.components())).collect(Collectors.toSet());
		if (!preciousComponents.isEmpty()) throw new ProfilePreciousException(preciousComponents, profile.components());
		profiles.remove(profileId);
		return profile;
	}

	private void switchProfile(SwitchyProfile nextProfile, ServerPlayer player, Component greeting) throws NbtException {
		SwitchyProfile currentProfile = profiles.get(current); // about to update manually
		boolean selfSwitch = currentProfile == nextProfile;
		// Read Components
		CompoundTag playerNbt;
		if (selfSwitch) {
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((AccessServerPlayer) player).getServer().registryAccess());
			player.saveWithoutId(output);
			playerNbt = output.buildResult();
		} else {
			playerNbt = updateFromPlayer(currentProfile, player);
		}
		// Mutate NBT
		for (SwitchyComponentType<?> componentType : nextProfile.components().keySet()) {
			componentType.tryMutate(nextProfile.components(), playerNbt, player);
		}

		this.greeting = greeting;
		current = nextProfile.id();

		((SwitchyPlayer) player).switchy$hotSwap(playerNbt, SwitchyCommands.prefix()
			.append(Component.literal(selfSwitch ? "Updated current profile " : "Switching to ").withStyle(ChatFormatting.GRAY))
			.append(SwitchyCommands.getProfileText(player, nextProfile))
			.append(Component.literal("! Please reconnect.").withStyle(ChatFormatting.GRAY))
		);
	}

	public static SwitchyPlayerData fromNbt(RegistryAccess registryManager, ValueInput input) {
		if (SwitchyComponentTypes.instance() == null) {
			throw new IllegalStateException("Can't load switchy data while the types aren't loaded!");
		}
		return input.read(Switchy.ID, SwitchyPlayerData.codec(SwitchyComponentTypes.instance())).orElse(null);
	}

	public void writeNbt(RegistryAccess registryManager, ValueOutput playerNbt) {
		if (SwitchyComponentTypes.instance() == null) {
			throw new IllegalStateException("Can't save switchy data while the types aren't loaded!");
		}
		if (size() > 1 || componentTypes.size() != SwitchyComponentTypes.instance().keys().size() || !profiles.containsKey("default") || componentTypes.stream().filter(t -> t.nbtReader() == null && t.playerReader() == null).anyMatch(t -> profiles.values().stream().anyMatch(p -> p.contains(t)))) {
			playerNbt.store(Switchy.ID, SwitchyPlayerData.codec(SwitchyComponentTypes.instance()), this);
		}
	}

	public void switchOrCreateProfile(String profileId, ServerPlayer player, Component greeting) throws NbtException {
		SwitchyProfile nextProfile = getOrCreateProfile(profileId.toLowerCase(), player);
		if (nextProfile.id().equals(current)) throw new ProfileCurrentException(nextProfile.id());
		switchProfile(nextProfile, player, greeting);
	}

	public void selfSwitch(SwitchyProfile currentProfile, ServerPlayer player, Component greeting) throws NbtException {
		if (!currentProfile.id().equals(current)) throw new ProfileCurrentException(currentProfile.id()); // profile must be current to self-switch
		switchProfile(currentProfile, player, greeting);
	}
}

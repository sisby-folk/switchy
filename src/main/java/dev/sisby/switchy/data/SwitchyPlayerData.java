package dev.sisby.switchy.data;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import dev.sisby.switchy.util.DispatchMapCodec;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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
			TextCodecs.CODEC.optionalFieldOf("greeting").forGetter(SwitchyPlayerData::greeting),
			types.SET_CODEC.fieldOf("componentTypes").forGetter(p -> p.componentTypes),
			DispatchMapCodec.of(Codecs.NON_EMPTY_STRING, id -> SwitchyProfile.codec(types, id)).fieldOf("profiles").xmap(a -> (Map<String, SwitchyProfile>) new HashMap<>(a), b -> b).forGetter(p -> p.profiles)
		).apply(instance, (current, optionalGreeting, componentTypes, profiles) -> new SwitchyPlayerData(current, optionalGreeting.orElse(null), componentTypes, profiles)));
	}

	private String current;
	private Text greeting;
	private final Set<SwitchyComponentType<?>> componentTypes;
	private final Map<String, SwitchyProfile> profiles;

	public SwitchyPlayerData(String current, Text greeting, Set<SwitchyComponentType<?>> componentTypes, Map<String, SwitchyProfile> profiles) {
		this.current = current;
		this.greeting = greeting;
		this.componentTypes = componentTypes;
		this.profiles = profiles;
	}

	public static SwitchyPlayerData of(ServerPlayerEntity player) {
		return ((SwitchyPlayer) player).switchy$getOrCreatePlayerData();
	}

	public static SwitchyPlayerData ofEarly(ServerPlayerEntity player) {
		return ((SwitchyPlayer) player).switchy$getPlayerData();
	}

	public static SwitchyPlayerData create(ServerPlayerEntity player, NbtCompound nbt) {
		SwitchyPlayerData data = new SwitchyPlayerData(
			"default",
			null,
			new LinkedHashSet<>(),
			new LinkedHashMap<>()
		);
		data.profiles.put("default", new SwitchyProfile("default", SwitchyComponentMap.empty()));
		for (SwitchyComponentType<?> componentType : Sets.difference(SwitchyComponentTypes.instance().values(), data.componentTypes)) {
			data.initComponent(componentType, player, nbt);
		}
		if (nbt.contains("switchy:presets", NbtElement.COMPOUND_TYPE)) data.recoverLegacyData(player, nbt.getCompound("switchy:presets"));
		return data;
	}

	public boolean profileExists(String profileId) {
		return profiles.containsKey(profileId);
	}

	public SwitchyProfile getCurrentProfile(ServerPlayerEntity player) throws NbtException {
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

	public Optional<Text> greeting() {
		return Optional.ofNullable(greeting);
	}

	public Text greet(ServerPlayerEntity player) {
		Text defaultedGreeting = Optional.ofNullable(greeting).orElseGet(() -> SwitchyCommands.prefix()
			.append(Text.literal("welcome back! current profile: ").formatted(Formatting.GRAY))
			.append(SwitchyCommands.getNameText(player, profiles.get(current)))
			.append(Text.literal(". ").formatted(Formatting.GRAY))
			.append(SwitchyCommands.clickable("list", "/switchy", true)));
		greeting = null;
		return defaultedGreeting;
	}

	public SwitchyProfile getProfile(String profileId, ServerPlayerEntity player) throws NbtException {
		if (profileId.equals(current)) updateFromPlayer(profiles.get(profileId), player);
		return profiles.get(profileId);
	}

	public int initComponents(Set<SwitchyComponentType<?>> types, ServerPlayerEntity player) {
		NbtCompound compound = new NbtCompound();
		player.writeNbt(compound);
		for (SwitchyComponentType<?> type : types) {
			if (initComponent(type, player, compound)) {
				types.add(type);
			} else { // roll back
				types.forEach(this::removeComponent);
				return 0;
			}
		}
		return types.size();
	}

	public boolean initComponent(SwitchyComponentType<?> componentType, ServerPlayerEntity player, NbtCompound nbt) {
		try {
			componentType.tryInitialize(profiles.values().stream().map(SwitchyProfile::components).toList(), nbt, player, player.getGameProfile().getName());
		} catch (Exception e) {
			Switchy.LOGGER.warn("Failed to initialize {} for {}", componentType.id(), player.getGameProfile().getName(), e);
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

	private static final Map<Identifier, Pair<String, String>> LEGACY_RECOVERIES = Map.ofEntries(
		Map.entry(SwitchyComponentTypes.INVENTORY, new Pair<>("switchy_inventories:inventories", "inventory")),
		Map.entry(SwitchyComponentTypes.ENDER_CHEST, new Pair<>("switchy_inventories:ender_chests", "inventory")),
		Map.entry(SwitchyComponentTypes.LEVEL, new Pair<>("switchy_inventories:experience", "experienceLevel")),
		Map.entry(SwitchyComponentTypes.XP, new Pair<>("switchy_inventories:experience", "experienceProgress")),
		Map.entry(SwitchyComponentTypes.TRINKETS_SLOTS, new Pair<>("switchy_inventories:trinkets", "trinkets:trinkets")),
		Map.entry(SwitchyComponentTypes.NAME_ID, new Pair<>("switchy:styled_nicknames", "styled_nickname")),
		Map.entry(SwitchyComponentTypes.TAILOR_SKIN, new Pair<>("switchy:fabric_tailor", "")),
		Map.entry(SwitchyComponentTypes.DIMENSION, new Pair<>("switchy_teleport:last_location", "last_location.dimension")),
		Map.entry(SwitchyComponentTypes.YAW, new Pair<>("switchy_teleport:last_location", "last_location.yaw")),
		Map.entry(SwitchyComponentTypes.PITCH, new Pair<>("switchy_teleport:last_location", "last_location.pitch")),
		Map.entry(SwitchyComponentTypes.SPAWN_X, new Pair<>("switchy_teleport:spawn_point", "respawn_point.x")),
		Map.entry(SwitchyComponentTypes.SPAWN_Y, new Pair<>("switchy_teleport:spawn_point", "respawn_point.y")),
		Map.entry(SwitchyComponentTypes.SPAWN_Z, new Pair<>("switchy_teleport:spawn_point", "respawn_point.z")),
		Map.entry(SwitchyComponentTypes.SPAWN_DIMENSION, new Pair<>("switchy_teleport:spawn_point", "respawn_point.dimension")),
		Map.entry(SwitchyComponentTypes.SPAWN_ANGLE, new Pair<>("switchy_teleport:spawn_point", "respawn_point.dimension")),
		Map.entry(SwitchyComponentTypes.SPAWN_FORCED, new Pair<>("switchy_teleport:spawn_point", "respawn_point.setSpawn")),
		Map.entry(SwitchyComponentTypes.HEALTH, new Pair<>("switchy_status:health", "healthValue")),
		Map.entry(SwitchyComponentTypes.EFFECTS, new Pair<>("switchy_status:status_effects", "status_effects")),
		Map.entry(SwitchyComponentTypes.FOOD, new Pair<>("switchy_status:hunger", "foodLevel")),
		Map.entry(SwitchyComponentTypes.SATURATION, new Pair<>("switchy_status:hunger", "foodSaturationLevel")),
		Map.entry(SwitchyComponentTypes.EXHAUSTION, new Pair<>("switchy_status:hunger", "exhaustion"))
	);

	public void recoverLegacyData(ServerPlayerEntity player, NbtCompound legacyData) {
		Switchy.LOGGER.warn("[Switchy] Found legacy switchy profiles in {}, performing data recovery...", player.getGameProfile().getName());
		try {
			File playerDataDir =  player.getServer().getSavePath(WorldSavePath.PLAYERDATA).toFile();
			File file = File.createTempFile(player.getUuidAsString() + "-switchy" + "-", ".dat_old", playerDataDir);
			NbtIo.writeCompressed(legacyData, file.toPath());
			File file2 = new File(playerDataDir, player.getUuidAsString() + "-switchy.dat_old");
			File file3 = new File(playerDataDir, player.getUuidAsString() + "-switchy.dat_older");
			Util.backupAndReplace(file2.toPath(), file.toPath(), file3.toPath());
			Switchy.LOGGER.info("[Switchy] Backed up legacy switchy data for {} to {}", player.getGameProfile().getName(), file2.getName());
		} catch (IOException e) { // allowing the game to keep running here would cause a data loss, so, don't
			Switchy.LOGGER.error("[Switchy] Failed to save switchy data backup for {}! Please manually back up and remove switchy:presets from the player.dat", player.getGameProfile().getName(), e);
			throw new CrashException(CrashReport.create(e, "Failed to save switchy data backup for %s!".formatted(player.getGameProfile().getName())));
		}
		// we're backed up, so make our best attempt.
		NbtCompound presets = legacyData.getCompound("list");
		boolean containsDefault = false;
		int recovered = 0;
		Set<Identifier> skippedTypeIds = new HashSet<>();
		for (String id : presets.getKeys()) {
			// make sure every related profile exists
			SwitchyProfile profile = getOrCreateProfile(id.toLowerCase(), player);
			if (profile.id().equals("default")) containsDefault = true;
			try { // try copy precious data for each
				NbtCompound modules = presets.getCompound(id);
				// simple cases
				for (Identifier typeId : LEGACY_RECOVERIES.keySet()) {
					SwitchyComponentType<?> type = SwitchyComponentTypes.instance().get(typeId);
					NbtCompound moduleCompound = modules.getCompound(LEGACY_RECOVERIES.get(typeId).getLeft());
					if (type == null /* || moduleCompound == null */) continue;
					NbtPathArgumentType.NbtPath path = NbtPathArgumentType.nbtPath().parse(new StringReader((LEGACY_RECOVERIES.get(typeId).getRight())));
					try {
						NbtElement element = path.get(moduleCompound).get(0);
						type.codec().parse(player.getServer().getRegistryManager().getOps(NbtOps.INSTANCE), element).result().ifPresent(v -> profile.set(type, v));
						recovered++;
					} catch (CommandSyntaxException e) {
						skippedTypeIds.add(typeId);
					}
				}
				// origin different nesting
				SwitchyComponentType<?> originType = SwitchyComponentTypes.instance().get(SwitchyComponentTypes.ORIGINS_ORIGIN);
				if (originType != null) {
					NbtElement layers = modules.getCompound("switchy:origins").get("OriginLayers");
					if (layers instanceof NbtList list && !list.isEmpty()) {
						NbtCompound originsCompound = new NbtCompound();
						originsCompound.put("OriginLayers", list);
						originsCompound.putBoolean("HadOriginBefore", true);
						originsCompound.putBoolean("SelectingOrigin", false);
						profile.set(originType, originsCompound);
						recovered++;
					}
				}
				// powers different naming
				SwitchyComponentType<?> powersType = SwitchyComponentTypes.instance().get(SwitchyComponentTypes.ORIGINS_POWERS);
				if (powersType != null) {
					NbtElement layers = modules.getCompound("switchy:apoli").get("PowerData");
					if (layers instanceof NbtList list && !list.isEmpty()) {
						NbtCompound powersCompound = new NbtCompound();
						powersCompound.put("Powers", list);
						profile.set(powersType, powersCompound);
						recovered++;
					}
				}
				// pos vec3d structuring
				SwitchyComponentType<?> positionType = SwitchyComponentTypes.instance().get(SwitchyComponentTypes.POS);
				if (positionType != null) {
					NbtElement layers = modules.getCompound("switchy_teleport:last_location").get("last_location");
					if (layers instanceof NbtCompound compound && !compound.isEmpty()) {
						Vec3d position = new Vec3d(compound.getFloat("x"), compound.getFloat("y"), compound.getFloat("z"));
						profile.set(positionType, position);
						recovered++;
					}
				}
			} catch (Exception e) {
				Switchy.LOGGER.error("[Switchy] Failed to recover legacy precious data {} of {}, please manually recover via -switchy.dat_old", id, player.getGameProfile().getName(), e);
			}
		}
		current = legacyData.getString("current").toLowerCase();
		if (!containsDefault) profiles.remove("default");
		Switchy.LOGGER.info("[Switchy] Finished recovering {} components from {} legacy switchy profiles for {}. Skipped: {}", recovered, presets.getSize(), player.getGameProfile().getName(), skippedTypeIds);
	}

	public SwitchyProfile getOrCreateProfile(String profileId, ServerPlayerEntity player) {
		if (profileExists(profileId)) return profiles.get(profileId);
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		SwitchyComponentMap components = SwitchyComponentMap.empty();
		for (SwitchyComponentType<?> componentType : componentTypes) {
			try {
				componentType.tryInitialize(List.of(components), nbt, player, profileId);
			} catch (Exception e) {
				Switchy.LOGGER.warn("Failed to initialize {} for {} profile {}", componentType.id(), player.getGameProfile().getName(), profileId, e);
			}
		}
		SwitchyProfile newProfile = new SwitchyProfile(profileId, components);
		profiles.put(profileId, newProfile);
		return newProfile;
	}

	public void validate(ServerPlayerEntity self, NbtCompound nbt) {
		Set<Identifier> groupsChecked = new HashSet<>();
		for (SwitchyComponentType<?> type : new HashSet<>(componentTypes)) {
			Identifier group = type.group();
			if (group != null && !groupsChecked.contains(group)) {
				groupsChecked.add(group);
				for (SwitchyComponentType<?> otherType : SwitchyComponentTypes.instance().values()) {
					if (group.equals(otherType.group()) && !componentTypes.contains(otherType)) {
						Switchy.LOGGER.info("[Switchy] Enabling component {} of partially enabled group {} for user {}", otherType.id(), group, self.getGameProfile().getName());
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

	public String randomBesides(String current, Random random) {
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

	public int importProfiles(List<ProfileImportData> profileData, ServerPlayerEntity player, @Nullable String name, boolean allowNew, Function<Integer, Text> greetingGetter) throws NbtException {
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
				quickTextEscape(Objects.requireNonNullElse(name, player.getGameProfile().getName())),
				quickTextEscape(data.description() == null ? "" : " | " + data.description()),
				quickTextEscape(Objects.requireNonNullElse(data.color(), "FFFFFF")),
				quickTextEscape(Objects.requireNonNullElse(data.display_name(), id).replace(bracketed, "")).trim());
			if (!newName.equals(profile.get(SwitchyComponentTypes.NAME))) {
				if (current.equals(id)) newCurrent = profile;
				profile.set(SwitchyComponentTypes.NAME, newName);
			}
			if (data.components() != null) {
				for (String componentKey : data.components().keySet()) {
					SwitchyComponentType<?> type = profile.components().keySet().stream().filter(t -> t.id().toString().equals(componentKey)).findFirst().orElse(null);
					if (type != null && type.importable()) {
						if (current.equals(id)) newCurrent = profile;
						type.decode(player.getServer().getRegistryManager().getOps(JsonOps.INSTANCE), data.components.get(componentKey), profile.components());
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

	private NbtCompound updateFromPlayer(SwitchyProfile profile, ServerPlayerEntity player) throws NbtException {
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		for (SwitchyComponentType<?> componentType : componentTypes) {
			if (componentType.nbtReader() != null) {
				profile.components().set(componentType, componentType.nbtReader().read(player.getServer().getRegistryManager(), nbt));
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

	private void switchProfile(SwitchyProfile nextProfile, ServerPlayerEntity player, Text greeting) throws NbtException {
		SwitchyProfile currentProfile = profiles.get(current); // about to update manually
		boolean selfSwitch = currentProfile == nextProfile;
		// Read Components
		NbtCompound playerNbt;
		if (selfSwitch) {
			playerNbt = new NbtCompound();
			player.writeNbt(playerNbt);
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
			.append(Text.literal(selfSwitch ? "Updated current profile " : "Switching to ").formatted(Formatting.GRAY))
			.append(SwitchyCommands.getNameText(player, nextProfile))
			.append(Text.literal("! Please reconnect.").formatted(Formatting.GRAY))
		);
	}

	public static SwitchyPlayerData fromNbt(DynamicRegistryManager registryManager, NbtCompound playerNbt) {
		if (SwitchyComponentTypes.instance() == null) {
			throw new IllegalStateException("Can't load switchy data while the types aren't loaded!");
		}
		return SwitchyPlayerData.codec(SwitchyComponentTypes.instance()).parse(registryManager.getOps(NbtOps.INSTANCE), playerNbt.getCompound(Switchy.ID)).resultOrPartial(Switchy.LOGGER::error).orElse(null);
	}

	public void writeNbt(DynamicRegistryManager registryManager, NbtCompound playerNbt) {
		if (SwitchyComponentTypes.instance() == null) {
			throw new IllegalStateException("Can't save switchy data while the types aren't loaded!");
		}
		if (size() > 1 || componentTypes.size() != SwitchyComponentTypes.instance().keys().size() || !profiles.containsKey("default") || componentTypes.stream().filter(t -> t.nbtReader() == null && t.playerReader() == null).anyMatch(t -> profiles.values().stream().anyMatch(p -> p.contains(t)))) {
			playerNbt.put(Switchy.ID, SwitchyPlayerData.codec(SwitchyComponentTypes.instance()).encodeStart(registryManager.getOps(NbtOps.INSTANCE), this).resultOrPartial(Switchy.LOGGER::error).orElse(null));
		}
	}

	public void switchOrCreateProfile(String profileId, ServerPlayerEntity player, Text greeting) throws NbtException {
		SwitchyProfile nextProfile = getOrCreateProfile(profileId.toLowerCase(), player);
		if (nextProfile.id().equals(current)) throw new ProfileCurrentException(nextProfile.id());
		switchProfile(nextProfile, player, greeting);
	}

	public void selfSwitch(SwitchyProfile currentProfile, ServerPlayerEntity player, Text greeting) throws NbtException {
		if (!currentProfile.id().equals(current)) throw new ProfileCurrentException(currentProfile.id()); // profile must be current to self-switch
		switchProfile(currentProfile, player, greeting);
	}
}

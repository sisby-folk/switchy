package dev.sisby.switchy.data;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SwitchyPlayerData {
	private static Codec<SwitchyPlayerData> codec(SwitchyComponentTypes types) {
		return RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("current").forGetter(SwitchyPlayerData::current),
			Codec.STRING.fieldOf("previous").forGetter(SwitchyPlayerData::previous),
			types.SET_CODEC.fieldOf("componentTypes").forGetter(p -> p.componentTypes),
			DispatchMapCodec.of(Codecs.NON_EMPTY_STRING, id -> SwitchyProfile.codec(types, id)).fieldOf("profiles").xmap(a -> (Map<String, SwitchyProfile>) new HashMap<>(a), b -> b).forGetter(p -> p.profiles)
		).apply(instance, SwitchyPlayerData::new));
	}

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
		return ((SwitchyPlayer) player).switchy$getOrCreatePlayerData();
	}

	public static SwitchyPlayerData ofEarly(ServerPlayerEntity player) {
		return ((SwitchyPlayer) player).switchy$getPlayerData();
	}

	public static SwitchyPlayerData create(ServerPlayerEntity player, NbtCompound nbt) {
		SwitchyPlayerData data = new SwitchyPlayerData(
			"default",
			"",
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

	public SwitchyProfile getCurrentProfile() {
		return profiles.get(current());
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

	public String previous() {
		return previous;
	}

	public SwitchyProfile getProfile(String profileId) {
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
		Map.entry(SwitchyComponentTypes.FOOD, new Pair<>("switchy_status:hunger", "foodLevel")),
		Map.entry(SwitchyComponentTypes.SATURATION, new Pair<>("switchy_status:hunger", "foodSaturationLevel")),
		Map.entry(SwitchyComponentTypes.EXHAUSTION, new Pair<>("switchy_status:hunger", "exhaustion")),
		Map.entry(SwitchyComponentTypes.NAME_ID, new Pair<>("switchy:styled_nicknames", "styled_nickname")),
		Map.entry(SwitchyComponentTypes.TAILOR_SKIN, new Pair<>("switchy:fabric_tailor", ""))
	);

	public void recoverLegacyData(ServerPlayerEntity player, NbtCompound legacyData) {
		Switchy.LOGGER.warn("[Switchy] Found legacy switchy profiles in {}, performing data recovery...", player.getGameProfile().getName());
		try {
			File playerDataDir =  player.getServer().getSavePath(WorldSavePath.PLAYERDATA).toFile();
			File file = File.createTempFile(player.getUuidAsString() + "-switchy" + "-", ".dat_old", playerDataDir);
			NbtIo.writeCompressed(legacyData, file);
			File file2 = new File(playerDataDir, player.getUuidAsString() + "-switchy.dat_old");
			File file3 = new File(playerDataDir, player.getUuidAsString() + "-switchy.dat_older");
			Util.backupAndReplace(file2, file, file3);
			Switchy.LOGGER.info("[Switchy] Backed up legacy switchy data for {} to {}", player.getGameProfile().getName(), file2.getName());
		} catch (IOException e) { // allowing the game to keep running here would cause a data loss, so, don't
			Switchy.LOGGER.error("[Switchy] Failed to save switchy data backup for {}! Please manually back up and remove switchy:presets from the player.dat", player.getGameProfile().getName(), e);
			throw new CrashException(CrashReport.create(e, "Failed to save switchy data backup for %s!".formatted(player.getGameProfile().getName())));
		}
		// we're backed up, so make our best attempt.
		NbtCompound presets = legacyData.getCompound("list");
		boolean containsDefault = false;
		for (String id : presets.getKeys()) {
			// make sure every related profile exists
			SwitchyProfile profile = getOrCreateProfile(id.toLowerCase(), player);
			if (profile.id().equals("default")) containsDefault = true;
			try { // try copy precious data for each
				NbtCompound modules = presets.getCompound(id);
				for (Identifier typeId : LEGACY_RECOVERIES.keySet()) {
					SwitchyComponentType<?> type = SwitchyComponentTypes.instance().get(typeId);
					if (type == null) continue;
					NbtElement element = LEGACY_RECOVERIES.get(typeId).getRight().isEmpty() ? modules.getCompound(LEGACY_RECOVERIES.get(typeId).getLeft()) : modules.getCompound(LEGACY_RECOVERIES.get(typeId).getLeft()).get(LEGACY_RECOVERIES.get(typeId).getRight());
					if (element == null) continue;
					type.codec().parse(NbtOps.INSTANCE, element).result().ifPresent(v -> profile.set(type, v));
				}
			} catch (Exception e) {
				Switchy.LOGGER.error("[Switchy] Failed to recover legacy precious data {} of {}, please manually recover via -switchy.dat_old", id, player.getGameProfile().getName(), e);
			}
		}
		current = legacyData.getString("current").toLowerCase();
		if (!containsDefault) profiles.remove("default");
		Switchy.LOGGER.info("[Switchy] Finished recovering {} legacy switchy profiles for {}.", presets.getSize(), player.getGameProfile().getName());
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

	public record ProfileImportData(String name, @Nullable String display_name, @Nullable String color, @Nullable String pronouns) {}

	private static final Pattern PARENTHESES = Pattern.compile("([<(\\[][^>)\\]]*[)>\\]])");

	public int importProfiles(List<ProfileImportData> profileData, ServerPlayerEntity player, @Nullable String name, boolean allowNew) {
		int updated = 0;
		for (ProfileImportData data : profileData) {
			String id = data.name().toLowerCase();
			if (!allowNew && !keySet().contains(id)) continue;
			updated++;
			SwitchyProfile profile = getOrCreateProfile(id, player);
			StringBuilder bracketed = new StringBuilder();
			if (data.display_name() != null) { // discord is better with long names. let's put it in the bio instead
				Matcher matcher = PARENTHESES.matcher(data.display_name());
				while (matcher.find()) {
					bracketed.append(matcher.group());
				}
			}
			profile.set(SwitchyComponentTypes.NAME, "<hover:'%s%s | %s'><#%s>%s".formatted(
				bracketed.isEmpty() ? "" : bracketed + (data.pronouns() != null ? " - " : ""),
				Objects.requireNonNullElse(data.pronouns(), ""),
				Objects.requireNonNullElse(name, player.getGameProfile().getName()),
				Objects.requireNonNullElse(data.color(), "FFFFFF"),
				Objects.requireNonNullElse(data.display_name(), id).replace(bracketed, "")).trim()
			);
		}
		return updated;
	}

	private NbtCompound updateFromPlayer(SwitchyProfile profile, ServerPlayerEntity player) throws NbtException {
		NbtCompound nbt = new NbtCompound();
		player.writeNbt(nbt);
		for (SwitchyComponentType<?> componentType : componentTypes) {
			if (componentType.nbtReader() != null) {
				profile.components().set(componentType, componentType.nbtReader().read(nbt));
			} else if (componentType.playerReader() != null) {
				profile.components().set(componentType, componentType.playerReader().read(player, profile.id()));
			}
		}
		return nbt;
	}

	public void updateCurrent(ServerPlayerEntity player) throws NbtException {
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

	private void switchProfile(SwitchyProfile nextProfile, ServerPlayerEntity player) throws NbtException {
		if (nextProfile.id().equals(current)) throw new ProfileCurrentException(nextProfile.id());
		SwitchyProfile currentProfile = getCurrentProfile();
		// Read Components
		NbtCompound playerNbt = updateFromPlayer(currentProfile, player);
		// Mutate NBT
		for (SwitchyComponentType<?> componentType : nextProfile.components().keySet()) {
			componentType.tryMutate(nextProfile.components(), playerNbt, player);
		}

		previous = current;
		current = nextProfile.id();

		((SwitchyPlayer) player).switchy$hotSwap(playerNbt, SwitchyCommands.prefix()
			.append(Text.literal("Switching to ").formatted(Formatting.GRAY))
			.append(SwitchyComponentTypes.NAME.asText(nextProfile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)))
			.append(Text.literal("! Please reconnect.").formatted(Formatting.GRAY))
		);
	}

	public static SwitchyPlayerData fromNbt(NbtCompound playerNbt) {
		if (SwitchyComponentTypes.instance() == null) {
			throw new IllegalStateException("Can't load switchy data while the types aren't loaded!");
		}
		return SwitchyPlayerData.codec(SwitchyComponentTypes.instance()).parse(NbtOps.INSTANCE, playerNbt.getCompound(Switchy.ID)).getOrThrow(true, Switchy.LOGGER::error);
	}

	public void writeNbt(NbtCompound playerNbt) {
		if (SwitchyComponentTypes.instance() == null) {
			throw new IllegalStateException("Can't save switchy data while the types aren't loaded!");
		}
		if (size() > 1) {
			playerNbt.put(Switchy.ID, SwitchyPlayerData.codec(SwitchyComponentTypes.instance()).encodeStart(NbtOps.INSTANCE, this).getOrThrow(true, Switchy.LOGGER::error));
		}
	}

	public SwitchyProfile switchOrCreateProfile(String profileId, ServerPlayerEntity player) throws NbtException {
		switchProfile(getOrCreateProfile(profileId.toLowerCase(), player), player);
		return getCurrentProfile();
	}

	public void clearPrevious() {
		previous = "";
	}
}

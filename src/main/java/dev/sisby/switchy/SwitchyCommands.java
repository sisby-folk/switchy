package dev.sisby.switchy;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import com.mojang.util.UUIDTypeAdapter;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.exception.NbtException;
import dev.sisby.switchy.exception.ProfileCurrentException;
import dev.sisby.switchy.exception.ProfileMissingException;
import dev.sisby.switchy.exception.ProfilePreciousException;
import dev.sisby.switchy.util.TypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SwitchyCommands {
	private static final Pattern COLOR_PATTERN = Pattern.compile("<(?:color:)?#([0-9a-fA-f]{6})>", Pattern.CASE_INSENSITIVE);
	private static final Pattern BIO_PATTERN = Pattern.compile("<hover:'?((?:\\\\'|.)*?)'?>", Pattern.CASE_INSENSITIVE);
	private static final Pattern BRACKETED_PATTERN = Pattern.compile("(\\([^()]+\\))", Pattern.CASE_INSENSITIVE);
	private static final String EXISTING = "existing";
	private static final String ALL = "all";

	public static void greet(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		SwitchyPlayerData data = SwitchyPlayerData.ofEarly(handler.getPlayer());
		if (data == null) return;
		handler.getPlayer().sendMessage(data.greet());
	}

	private static int list(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		List<String> profiles = Stream.concat(Sets.difference(data.keySet(), Set.of(data.current())).stream().sorted(), Stream.of(data.current())).toList();

		feedback.accept(prefix()
			.append(Text.literal("you have ").formatted(Formatting.GRAY))
			.append(Text.literal("%s".formatted(data.size())).formatted(Formatting.WHITE))
			.append(Text.literal(" profile%s available. ".formatted(data.size() == 1 ? "" : "s")).formatted(Formatting.GRAY))
			.append(clickable("new", "/switchy new ", false))
		);

		for (String id : profiles) {
			SwitchyProfile profile;
			try {
				profile = data.getProfile(id, player);
			} catch (NbtException e) {
				throw new RuntimeException(e);
			}
			feedback.accept(indent()
				.append(profile.id().equals(data.current()) ? Text.literal("current").formatted(Formatting.GRAY) : clickable("switch", "/switch %s".formatted(StringArgumentType.escapeIfRequired(profile.id())), true))
				.append(" ")
				.append(clickable("edit", "/switchy edit %s ".formatted(StringArgumentType.escapeIfRequired(profile.id())), false))
				.append(" ")
				.append(SwitchyComponentTypes.NAME.asText(profile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)).setStyle(Style.EMPTY
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Texts.join(profile.asTexts(player), Text.of("\n")).copy().append("\n").append(Text.literal("... /switchy view %s".formatted(StringArgumentType.escapeIfRequired(profile.id()))).formatted(Formatting.AQUA))))
					.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/switchy view %s".formatted(StringArgumentType.escapeIfRequired(profile.id()))))
				))
			);
		}

		if (data.size() == 1 && data.current().equals("default")) {
			feedback.accept(indent()
				.append(Text.literal("HINT: ").formatted(Formatting.LIGHT_PURPLE))
				.append(Text.literal("rename your first profile: ").formatted(Formatting.GRAY))
				.append(clickable("/switchy edit default id [...]", "/switchy edit default id ", false, Formatting.AQUA, "", ""))
			);
		} else if (data.size() == 1) {
			feedback.accept(indent()
				.append(Text.literal("HINT: ").formatted(Formatting.LIGHT_PURPLE))
				.append(Text.literal("configure profile shared data via ").formatted(Formatting.GRAY))
				.append(clickable("/switchy components", "/switchy components", true, Formatting.AQUA, "", ""))
			);
		}

		return data.size();
	}

	private static int components(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		Map<Identifier, List<SwitchyComponentType<?>>> grouped = SwitchyComponentTypes.grouped(SwitchyComponentTypes.instance().values());
		long numEnabled = grouped.values().stream().filter(g -> data.componentSet().contains(g.get(0))).count();
		feedback.accept(prefix()
			.append(Text.literal("you're switching ").formatted(Formatting.GRAY))
			.append(Text.literal("%d".formatted(numEnabled).formatted(Formatting.WHITE)))
			.append(Text.literal(" component%s and sharing ".formatted(numEnabled == 1 ? "" : "s")).formatted(Formatting.GRAY))
			.append(Text.literal("%d".formatted(grouped.size() - numEnabled)).formatted(Formatting.WHITE))
			.append(Text.literal(".").formatted(Formatting.GRAY))
		);
		grouped.forEach((id, types) -> {
			boolean enabled = data.componentSet().contains(types.get(0));
			List<SwitchyProfile> matchingProfiles = data.values().stream().sorted(Comparator.comparing(SwitchyProfile::id)).filter(p -> types.stream().anyMatch(t -> p.get(t) != null && (t.emptyChecker() == null || t.isPrecious(p.components())))).toList();
			feedback.accept(indent()
				.append(enabled ?
					clickable(Text.literal("enabled").formatted(Formatting.GREEN), "/switchy components disable %s".formatted(id), data.size() == 1 ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, Text.empty().append(Text.literal("Click to share ").formatted(Formatting.GRAY)).append(id.getPath()).append(Text.literal(" between profiles.").formatted(Formatting.GRAY)).append(data.size() == 1 ? Text.empty() : Text.literal("\n").append(Text.literal("this deletes data from other profiles!").formatted(Formatting.GOLD))), Formatting.RED, "[", "]") :
					clickable(Text.literal("disabled").formatted(Formatting.RED), "/switchy components enable %s".formatted(id), ClickEvent.Action.RUN_COMMAND, Text.empty().append(Text.literal("click to switch ").formatted(Formatting.GRAY)).append(id.getPath()).append(Text.literal(" per-profile.").formatted(Formatting.GRAY)), Formatting.GREEN, "[", "]")
				)
				.append(" ")
				.append(id.getPath()).setStyle(Style.EMPTY
					.withColor(enabled ? Formatting.WHITE : Formatting.DARK_GRAY)
					.withHoverEvent(!enabled ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, matchingProfiles.isEmpty() ? Text.literal("<no %s data yet>".formatted(id.getPath().replace("_", " "))).formatted(Formatting.GRAY) : Texts.join(matchingProfiles.stream().map(p -> Text.empty()
						.append(Text.literal(p.id()).formatted(Formatting.GRAY))
						.append(": ")
						.append(Texts.join(types.stream().filter(t -> p.get(t) != null).map(t -> t.asText(p.components())).toList(), Text.literal(", ").formatted(Formatting.GRAY)))
					).toList(), Text.of("\n"))))
				)
			);
		});
		if (data.size() == 1) {
			feedback.accept(indent()
				.append(Text.literal("HINT: ").formatted(Formatting.LIGHT_PURPLE))
				.append(Text.literal("create your second profile via ").formatted(Formatting.GRAY))
				.append(clickable("/switchy new [name]", "/switchy new ", false, Formatting.AQUA, "", ""))
			);
		}
		return data.componentSet().size();
	}

	public record PlayerImportData(@Nullable String name, List<SwitchyPlayerData.ProfileImportData> members, @Nullable List<SwitchyPlayerData.GroupImportData> groups) {}

	private static int importProfiles(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String url, String scope) {
		int beforeSize = data.size();
		PlayerImportData importData;
		try {
			importData = new Gson().fromJson(new InputStreamReader(new URL(url).openStream()), PlayerImportData.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Function<Integer, Text> feedbackGetter = updated -> prefix()
			.append(!EXISTING.equals(scope) ? Text.empty()
				.append(Text.literal("created ").formatted(Formatting.GREEN))
				.append(Text.literal("%d".formatted(data.size() - beforeSize)).formatted(Formatting.WHITE))
				.append(Text.literal(" profile%s and ".formatted((data.size() - beforeSize) == 1 ? "" : "s")).formatted(Formatting.GREEN)) : Text.empty()
			)
			.append(Text.literal("updated ").formatted(Formatting.GREEN))
			.append(Text.literal("%d".formatted(updated - (data.size() - beforeSize))).formatted(Formatting.WHITE))
			.append(Text.literal("%s profile%s. ".formatted(!EXISTING.equals(scope) ? " existing" : "", (updated - (data.size() - beforeSize)) == 1 ? "" : "s")).formatted(Formatting.GREEN))
			.append(clickable("list", "/switchy", true));
		List<SwitchyPlayerData.ProfileImportData> profilesToImport = importData.members();
		if (!EXISTING.equals(scope) && !ALL.equals(scope)) {
			SwitchyPlayerData.GroupImportData group = Objects.requireNonNullElse(importData.groups, new ArrayList<SwitchyPlayerData.GroupImportData>()).stream().filter(g -> g.name().equals(scope)).findAny().orElse(null);
			SwitchyPlayerData.ProfileImportData profile = importData.members().stream().filter(p -> p.name().equals(scope)).findAny().orElse(null);
			if (group != null) {
				profilesToImport = importData.members().stream().filter(p -> group.members().contains(p.id())).toList();
			} else if (profile != null) {
				profilesToImport = List.of(profile);
			} else {
				profilesToImport = List.of();
			}
		}
		try {
			int updated = data.importProfiles(profilesToImport, player, importData.name(), !EXISTING.equals(scope), feedbackGetter);
			feedback.accept(feedbackGetter.apply(updated));
			return data.size() - beforeSize;
		} catch (Exception e) {
			feedback.accept(prefix()
				.append("error while switching: ").formatted(Formatting.RED)
				.append(Objects.requireNonNullElse(e.getMessage(), "???")).formatted(Formatting.GRAY)
				.append(" see server logs for more info.").formatted(Formatting.RED)
			);
			Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", data.current(), player.getGameProfile().getName(), e);
			return 0;
		}
	}

	private static String quickTextInscape(String input) {
		return input.replace("\\<", "<").replace("\\'", "'");
	}

	private static int export(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		String sysName = null;
		List<SwitchyPlayerData.ProfileImportData> members = new ArrayList<>();
		try {
			for (String profileId : data.keySet()) {
				SwitchyProfile profile = data.getProfile(profileId, player);
				Map<String, JsonElement> components = new HashMap<>();
				// encode importables
				for (SwitchyComponentType<?> type : profile.components().keySet()) {
					if (type.importable()) {
						type.encode(JsonOps.INSTANCE, profile.components()).ifPresent(e -> components.put(type.id().toString(), e));
					}
				}
				// attempt to rip PK name data
				String name = profile.get(SwitchyComponentTypes.NAME);
				String color = null;
				String description = null;
				String pronouns = null;
				StringBuilder bracketed = new StringBuilder(" ");
				if (name != null) {
					Matcher colorMatcher = COLOR_PATTERN.matcher(name);
					if (colorMatcher.find()) {
						color = quickTextInscape(colorMatcher.group(1));
					}
					String bio = "";
					Matcher bioMatcher = BIO_PATTERN.matcher(name);
					if (bioMatcher.find()) {
						bio = quickTextInscape(bioMatcher.group(1));
					}
					List<String> splitBio = Arrays.stream(bio.split(" \\| ")).toList();
					if (splitBio.size() > 1) {
						String remainder = splitBio.get(0);
						Matcher bracketedMatcher = BRACKETED_PATTERN.matcher(splitBio.get(0));
						while (bracketedMatcher.find()) {
							String group = bracketedMatcher.group(1);
							remainder = remainder.replace(group, "").trim();
							bracketed.append(group);
						}
						if (!remainder.isEmpty()) pronouns = remainder;
						if (!player.getGameProfile().getName().equals(splitBio.get(1))) sysName = splitBio.get(1);
					}
					if (splitBio.size() > 2) {
						description = splitBio.get(2);
					}
					name = (SwitchyComponentTypes.NAME.asText(name).getString() + bracketed).trim(); // strip tags
				}
				// bodge player renderer avatar from skin
				String avatarUrl = null;
				if (Switchy.CONFIG.exportAvatarUrl.contains("%s")) {
					String key = player.getGameProfile().getName();
					Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = player.getServer().getSessionService().getTextures(player.getGameProfile(), false);
					SwitchyComponentType<?> skinComponent = SwitchyComponentTypes.instance().get(SwitchyComponentTypes.TAILOR_SKIN);
					if (skinComponent != null && profile.contains(skinComponent) && profile.get(skinComponent) instanceof NbtCompound skinCompound && skinCompound.get("value") instanceof NbtString valueString) {
						Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
						MinecraftTexturesPayload payload = gson.fromJson(new String(Base64.getDecoder().decode(valueString.asString())), MinecraftTexturesPayload.class);
						textures = payload.getTextures();
					}
					MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
					if (skin != null) key = skin.getHash();
					avatarUrl = Switchy.CONFIG.exportAvatarUrl.formatted(key);
				}
				members.add(new SwitchyPlayerData.ProfileImportData(null, profileId, name, color, pronouns, description, avatarUrl, List.of(new SwitchyPlayerData.ProxyTag(profileId + ":", null)), components));
			}
			feedback.accept(prefix()
				.append(Text.literal("exported ").formatted(Formatting.GREEN))
				.append(Text.literal("%d".formatted(data.size())))
				.append(Text.literal(" profile%s. ".formatted(data.size() == 1 ? "" : "s")).formatted(Formatting.GREEN))
				.append(clickable("copy", SwitchyComponentTypes.GSON.toJson(new PlayerImportData(sysName, members, null)), ClickEvent.Action.COPY_TO_CLIPBOARD, Formatting.AQUA, "<", ">"))
			);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		return data.size();
	}

	private static int viewProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId) {
		SwitchyProfile profile;
		try {
			profile = data.getProfile(profileId, player);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		if (profile == null) {
			feedback.accept(prefix().append(Text.literal("profile doesn't exist!").formatted(Formatting.YELLOW)));
			return 0;
		}
		feedback.accept(prefix()
			.append(Text.literal("profile ").formatted(Formatting.GRAY))
			.append(profileId)
			.append(Text.literal(" contains ").formatted(Formatting.GRAY))
			.append("%d".formatted(profile.components().size()))
			.append(Text.literal(" component%s. ".formatted(profile.components().size() == 1 ? "" : "s")).formatted(Formatting.GRAY))
			.append(profileId.equals(data.current()) ? Text.empty() : clickable("switch", "/switch %s".formatted(StringArgumentType.escapeIfRequired(profileId)), true))
		);
		profile.components().asTexts().forEach(componentText -> feedback.accept(indent().append(componentText)));
		return profile.components().size();
	}

	private static int switchProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId, Boolean exists) {
		boolean reallyExists = data.profileExists(profileId);
		if (data.current().equals(profileId)) {
			feedback.accept(prefix()
				.append(Text.literal("profile '").formatted(Formatting.YELLOW))
				.append(profileId)
				.append(Text.literal("' already active! specify a different profile!").formatted(Formatting.YELLOW))
			);
			return 0;
		}
		if (exists == false && reallyExists) {
			feedback.accept(prefix()
				.append("that profile already exists! try ").formatted(Formatting.YELLOW)
				.append(clickable("/switch %s".formatted(profileId), "/switch %s".formatted(profileId), true, Formatting.AQUA, "", ""))
			);
			return 0;
		}
		if (exists && !reallyExists) {
			feedback.accept(prefix()
				.append(Text.literal("profile ").formatted(Formatting.YELLOW))
				.append(profileId)
				.append(Text.literal(" hasn't been made yet!").formatted(Formatting.YELLOW))
			);
			feedback.accept(indent()
				.append(Text.literal("use ").formatted(Formatting.YELLOW))
				.append(clickable("/switchy new %s".formatted(profileId), "/switchy new %s".formatted(profileId), true, Formatting.AQUA, "", ""))
				.append(Text.literal(" to create it.").formatted(Formatting.YELLOW))
			);
			return 0;
		}
		try {
			SwitchyProfile currentProfile = data.getCurrentProfile(player);
			SwitchyProfile nextProfile = data.getOrCreateProfile(profileId, player);
			data.switchOrCreateProfile(profileId, player, prefix()
				.append(SwitchyComponentTypes.NAME.asText(currentProfile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)))
				.append(Text.literal(" \uD83E\uDC46 ").formatted(Formatting.GREEN))
				.append(SwitchyComponentTypes.NAME.asText(nextProfile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)))
				.append(Text.literal("! ").formatted(Formatting.GREEN))
				.append(clickable("list", "/switchy", true)));
		} catch (ProfileCurrentException e) {
			feedback.accept(prefix()
				.append(Text.literal("profile '").formatted(Formatting.YELLOW))
				.append(profileId)
				.append(Text.literal("' already active! specify a different profile!").formatted(Formatting.YELLOW))
			);
			return 0;
		} catch (Exception e) {
			feedback.accept(prefix()
				.append("error while switching: ").formatted(Formatting.RED)
				.append(Objects.requireNonNullElse(e.getMessage(), "???")).formatted(Formatting.GRAY)
				.append(" see server logs for more info.").formatted(Formatting.RED)
			);
			Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", profileId, player.getGameProfile().getName(), e);
			return 0;
		}
		return 1;
	}

	private static int switchNextProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		return switchProfile(player, data, feedback, data.profileAfter(data.current()), true);
	}

	private static int switchRandomProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		return switchProfile(player, data, feedback, data.randomBesides(data.current(), player.getRandom()), true);
	}

	public static <T> int editComponent(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId, SwitchyComponentType<T> type, T value) {
		if (!data.componentSet().contains(type)) {
			feedback.accept(prefix().append(Text.literal("can't edit a shared component!").formatted(Formatting.YELLOW)));
			return 0;
		}
		SwitchyProfile profile;
		try {
			profile = data.getProfile(profileId, player);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		T oldValue = profile.set(type, value);
		Text feedbackText = prefix()
			.append(Text.literal("edited ").formatted(Formatting.GREEN))
			.append(profileId)
			.append(Text.literal(":").formatted(Formatting.GRAY))
			.append(type.id().getPath())
			.append(Text.literal(" - ").formatted(Formatting.GREEN))
			.append(oldValue == null ? Text.of("empty") : type.asText(oldValue))
			.append(Text.literal(" \uD83E\uDC46 ").formatted(Formatting.GREEN))
			.append(type.asText(value))
			.append(Text.literal("!").formatted(Formatting.GREEN))
			.append(" ")
			.append(clickable("list", "/switchy", true));
		if (profileId.equals(data.current())) {
			try {
				data.selfSwitch(profile, player, feedbackText);
				return 2;
			} catch (Exception e) {
				feedback.accept(prefix()
					.append("error while self-switching: ").formatted(Formatting.RED)
					.append(Objects.requireNonNullElse(e.getMessage(), "???")).formatted(Formatting.GRAY)
					.append(" See server logs for more info.").formatted(Formatting.RED)
				);
				Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", profileId, player.getGameProfile().getName(), e);
				return 0;
			}
		} else {
			feedback.accept(feedbackText);
			return 1;
		}
	}

	private static int renameProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId, String newId) {
		try {
			data.renameProfile(profileId, newId);
		} catch (IllegalArgumentException e) {
			feedback.accept(prefix().append(Text.literal(e.getMessage()).formatted(Formatting.YELLOW)));
			return 0;
		}
		feedback.accept(prefix()
			.append(Text.literal("renamed ").formatted(Formatting.GREEN))
			.append(profileId)
			.append(Text.literal(" \uD83E\uDC46 ").formatted(Formatting.GREEN))
			.append(newId)
			.append(Text.literal("!").formatted(Formatting.GREEN))
			.append(" ")
			.append(clickable("list", "/switchy", true))
		);
		return 1;
	}

	private static int deleteProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId) {
		try {
			SwitchyProfile profile = data.deleteProfile(profileId);
			feedback.accept(prefix()
				.append(Text.literal("profile ").formatted(Formatting.GREEN))
				.append(profileId)
				.append(Text.literal(" deleted successfully!").formatted(Formatting.GREEN))
				.append(" ")
				.append(clickable("list", "/switchy", true))
			);
			return profile.components().size();
		} catch (ProfileCurrentException e) {
			feedback.accept(prefix().append(Text.literal("can't delete current profile!").formatted(Formatting.YELLOW)));
			return 0;
		} catch (ProfilePreciousException e) {
			feedback.accept(prefix()
				.append(Text.literal("profile ").formatted(Formatting.YELLOW))
				.append(profileId)
				.append(Text.literal(" contains ").formatted(Formatting.YELLOW))
				.append(String.valueOf(e.getPreciousComponents().size()))
				.append(Text.literal("x precious components!").formatted(Formatting.YELLOW))
			);
			e.getPreciousComponents().asTexts().forEach(componentText -> feedback.accept(indent().append(componentText)));
			return 0;
		}
	}

	private static int enableComponent(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, Identifier id) {
		Set<SwitchyComponentType<?>> types = SwitchyComponentTypes.instance().values().stream().filter(t -> id.equals(t.group())).collect(Collectors.toSet());
		if (types.isEmpty() && SwitchyComponentTypes.instance().contains(id) && SwitchyComponentTypes.instance().get(id).group() == null) types.add(SwitchyComponentTypes.instance().get(id));
		if (types.isEmpty()) {
			feedback.accept(prefix().append(Text.literal("component doesn't exist!").formatted(Formatting.YELLOW)));
			return 0;
		}
		if (types.stream().anyMatch(t -> data.componentSet().contains(t))) {
			feedback.accept(prefix().append(Text.literal("component is already enabled!").formatted(Formatting.YELLOW)));
			return 0;
		}
		int changed = data.initComponents(types, player);
		if (changed == 0) {
			feedback.accept(prefix().append(Text.literal("component failed to initialize! see logs for more info").formatted(Formatting.RED)));
			return 0;
		}
		components("", player, data, feedback);
		feedback.accept(prefix().append(Text.literal(id.getPath())).append(Text.literal(" is now switched per-profile. ").formatted(Formatting.GREEN)).append(clickable("list", "/switchy", true)));
		return changed;
	}

	private static int disableComponent(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, Identifier id) {
		Set<SwitchyComponentType<?>> types = SwitchyComponentTypes.instance().values().stream().filter(t -> id.equals(t.group())).collect(Collectors.toSet());
		if (types.isEmpty() && SwitchyComponentTypes.instance().contains(id) && SwitchyComponentTypes.instance().get(id).group() == null) types.add(SwitchyComponentTypes.instance().get(id));
		if (types.isEmpty()) {
			feedback.accept(prefix().append(Text.literal("component doesn't exist!").formatted(Formatting.YELLOW)));
			return 0;
		}
		if (types.stream().anyMatch(t -> !data.componentSet().contains(t))) {
			feedback.accept(prefix().append(Text.literal("component is already disabled!").formatted(Formatting.YELLOW)));
			return 0;
		}
		int changed = data.removeComponents(types);
		if (changed == 0) {
			feedback.accept(prefix().append(Text.literal("component is precious! empty it first.").formatted(Formatting.YELLOW)));
			return 0;
		}
		components("", player, data, feedback);
		feedback.accept(prefix().append(Text.literal(id.getPath())).append(Text.literal(" is now shared between profiles. ").formatted(Formatting.GREEN)).append(clickable("list", "/switchy", true)));
		return changed;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registries, CommandManager.RegistrationEnvironment environment) {
		RequiredArgumentBuilder<ServerCommandSource, String> editBuilder = profile(true);
		for (SwitchyComponentType<?> type : SwitchyComponentTypes.getStatic().values()) {
			type.tryCreateEditor(arg -> editBuilder.then(CommandManager.literal(type.id().toString().replace("switchy:", "")).then(arg)));
		}

		dispatcher.register(
			CommandManager.literal("switch")
				.requires(c -> c.getPlayer() != null && SwitchyPlayerData.ofEarly(c.getPlayer()) != null && SwitchyPlayerData.ofEarly(c.getPlayer()).size() > 1)
				.then(CommandManager.literal("?")
					.executes(c -> execute(c, (i, p, d, f) -> switchRandomProfile(p, d, f)))
				)
				.then(profile(false)
					.executes(c -> execute(c, (i, p, d, f) -> switchProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase(), true)))
				)
				.executes(c -> execute(c, (i, p, d, f) -> switchNextProfile(p, d, f)))
		);
		dispatcher.register(
			CommandManager.literal("switchy")
				.then(CommandManager.literal("new")
					.then(CommandManager.argument("name", StringArgumentType.string())
						.executes(c -> execute(c, (i, p, d, f) -> switchProfile(p, d, f, c.getArgument("name", String.class).toLowerCase(), false)))
					)
				)
				.then(CommandManager.literal("view")
					.then(profile(true)
						.executes(c -> execute(c, (i, p, d, f) -> viewProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase())))
					)
				)
				.then(CommandManager.literal("delete")
					.then(profile(false)
						.executes(c -> execute(c, (i, p, d, f) -> deleteProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase())))
					)
				)
				.then(CommandManager.literal("edit")
					.then(editBuilder
						.then(CommandManager.literal("id")
							.then(
								CommandManager.argument("id", StringArgumentType.word()).executes(c -> execute(c, (i, p, d, f) -> renameProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase(), c.getArgument("id", String.class).toLowerCase()))))
						)
					)
				)
				.then(CommandManager.literal("import")
					.then(CommandManager.argument("scope", StringArgumentType.word())
						.suggests((c, b) -> CommandSource.suggestMatching(List.of(ALL, EXISTING), b))
						.then(CommandManager.argument("url", StringArgumentType.greedyString())
							.executes(c -> execute(c, (i, p, d, f) -> importProfiles(p, d, f, c.getArgument("url", String.class), c.getArgument("scope", String.class))))
						)
					)
				)
				.then(CommandManager.literal("export")
					.executes(c -> execute(c, SwitchyCommands::export))
				)
				.then(CommandManager.literal("components")
					.then(CommandManager.literal("disable")
						.then(groupedComponent(true)
							.executes(c -> execute(c, (i, p, d, f) -> disableComponent(p, d, f, c.getArgument("component", Identifier.class))))
						)
					)
					.then(CommandManager.literal("enable")
						.then(groupedComponent(false)
							.executes(c -> execute(c, (i, p, d, f) -> enableComponent(p, d, f, c.getArgument("component", Identifier.class))))
						)
					)
					.executes(c -> execute(c, SwitchyCommands::components))
				)
				.executes(c -> execute(c, SwitchyCommands::list))
		);
	}

	private static RequiredArgumentBuilder<ServerCommandSource, String> profile(boolean includeCurrent) {
		return CommandManager.argument("profile", StringArgumentType.string()).suggests((c, b) -> CommandSource.suggestMatching(
			(Iterable<String>) map(c, (i, p, d, f) -> (includeCurrent ? d.keySet() : Sets.difference(d.keySet(), Set.of(d.current()))).stream().map(StringArgumentType::escapeIfRequired).toList(), false), b));
	}

	private static RequiredArgumentBuilder<ServerCommandSource, Identifier> groupedComponent(Boolean enabled) {
		return CommandManager.argument("component", IdentifierArgumentType.identifier()).suggests((c, b) -> CommandSource.suggestIdentifiers(
			(Iterable<Identifier>) map(c, (i, p, d, f) -> SwitchyComponentTypes.instance().values().stream().filter(t -> enabled == null || (!enabled ^ d.componentSet().contains(t))).map(t -> Objects.requireNonNullElse(t.group(), t.id())).distinct().toList(), false), b));
	}

	private static RequiredArgumentBuilder<ServerCommandSource, Identifier> component(Boolean enabled) {
		return CommandManager.argument("component", IdentifierArgumentType.identifier()).suggests((c, b) -> CommandSource.suggestIdentifiers(
			(Iterable<Identifier>) map(c, (i, p, d, f) -> SwitchyComponentTypes.instance().values().stream().filter(t -> enabled == null || (!enabled ^ d.componentSet().contains(t))).map(TypeRegistry.Type::id).toList(), false), b));
	}

	public static MutableText prefix() {
		return Text.empty().append(Text.literal("[Switchy] ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText indent() {
		return Text.empty().append(Text.literal("|| ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText clickable(String name, String contents, ClickEvent.Action action, Formatting formatting, String prefix, String suffix) {
		return clickable(Text.literal(name).formatted(formatting), contents, action, null, formatting, prefix, suffix);
	}

	public static MutableText clickable(Text name, String contents, ClickEvent.Action action, Text hint, Formatting formatting, String prefix, String suffix) {
		return Text.empty()
			.append(Text.literal(prefix).formatted(Formatting.GRAY))
			.append(name.copy().styled(s -> s
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.empty().append(hint == null ? Text.empty() : hint.copy().append("\n")).append(Text.literal(contents + (action == ClickEvent.Action.SUGGEST_COMMAND ? "..." : "")).formatted(formatting))))
				.withClickEvent(new ClickEvent(action, contents))
			))
			.append(Text.literal(suffix).formatted(Formatting.GRAY));
	}

	public static MutableText clickable(String name, String contents, boolean instant, Formatting formatting, String prefix, String suffix) {
		return clickable(name, contents, instant ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, formatting, prefix, suffix);
	}

	public static MutableText clickable(String name, String command, boolean instant) {
		return clickable(name, command, instant, Formatting.AQUA, "<", ">");
	}

	public static <T> T map(CommandContext<ServerCommandSource> context, SurveyorCommandExecutor<T> executor, boolean feedback) {
		ServerPlayerEntity player;
		try {
			player = context.getSource().getPlayerOrThrow();
		} catch (CommandSyntaxException e) {
			if (feedback) Switchy.LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return null;
		}

		SwitchyPlayerData data = SwitchyPlayerData.of(player);
		try {
			return executor.execute(context.getInput(), player, data, t -> context.getSource().sendFeedback(() -> t, false));
		} catch (ProfileMissingException e) {
			context.getSource().sendFeedback(() -> prefix().append(Text.literal("profile doesn't exist!").formatted(Formatting.YELLOW)), false);
			return null;
		} catch (Exception e) {
			if (feedback) context.getSource().sendFeedback(() -> prefix().append(Text.literal("command \"/%s...\" failed! Check log for details.".formatted(context.getInput().substring(0, Math.min(context.getInput().length(), 20)))).formatted(Formatting.RED)), false);
			if (feedback) Switchy.LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return null;
		}
	}

	public static int execute(CommandContext<ServerCommandSource> context, SurveyorCommandExecutor<Integer> executor) {
		return Objects.requireNonNullElse(map(context, executor, true), 0);
	}

    public interface SurveyorCommandExecutor<T> {
		T execute(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) throws ProfileMissingException;
	}
}

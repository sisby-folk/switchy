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
import dev.sisby.switchy.compat.PlaceholderApiCompat;
import dev.sisby.switchy.compat.StyledChatCompat;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.duck.SwitchyGameProfile;
import dev.sisby.switchy.exception.NbtException;
import dev.sisby.switchy.exception.ProfileCurrentException;
import dev.sisby.switchy.exception.ProfileMissingException;
import dev.sisby.switchy.exception.ProfilePreciousException;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.TypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
	private static final String EXISTING = "existing";
	private static final String ALL = "all";

	public static void greet(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
		SwitchyPlayerData data = SwitchyPlayerData.ofEarly(handler.getPlayer());
		if (data == null) return;
		handler.getPlayer().sendSystemMessage(data.greet(handler.getPlayer()));
	}

	private static int list(String input, ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) {
		List<String> profiles = Stream.concat(Sets.difference(data.keySet(), Set.of(data.current())).stream().sorted(), Stream.of(data.current())).toList();

		feedback.accept(prefix()
			.append(Component.literal("you have ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("%s".formatted(data.size())).withStyle(ChatFormatting.WHITE))
			.append(Component.literal(" profile%s available. ".formatted(data.size() == 1 ? "" : "s")).withStyle(ChatFormatting.GRAY))
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
				.append(profile.id().equals(data.current()) ? Component.literal("current").withStyle(ChatFormatting.GRAY) : clickable("switch", "/switch %s".formatted(StringArgumentType.escapeIfRequired(profile.id())), true))
				.append(" ")
				.append(clickable("edit", "/switchy edit %s ".formatted(StringArgumentType.escapeIfRequired(profile.id())), false))
				.append(" ")
				.append(getProfileText(player, profile, false).withStyle(s -> s
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentUtils.formatList(profile.asTexts(player), Component.nullToEmpty("\n")).copy().append("\n").append(Component.literal("... /switchy view %s".formatted(StringArgumentType.escapeIfRequired(profile.id()))).withStyle(ChatFormatting.AQUA))))
					.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/switchy view %s".formatted(StringArgumentType.escapeIfRequired(profile.id()))))
				))
			);
		}

		if (data.size() == 1 && data.current().equals("default")) {
			feedback.accept(indent()
				.append(Component.literal("HINT: ").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal("rename your first profile: ").withStyle(ChatFormatting.GRAY))
				.append(clickable("/switchy edit default id [...]", "/switchy edit default id ", false, ChatFormatting.AQUA, "", ""))
			);
			hintClickables(feedback);
		} else if (data.size() == 1) {
			feedback.accept(indent()
				.append(Component.literal("HINT: ").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal("configure profile shared data via ").withStyle(ChatFormatting.GRAY))
				.append(clickable("/switchy components", "/switchy components", true, ChatFormatting.AQUA, "", ""))
			);
		}

		return data.size();
	}

	private static void hintClickables(Consumer<Component> feedback) {
		feedback.accept(indent()
			.append(Component.literal("note: switchy output is ").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("hoverable").withStyle(ChatFormatting.ITALIC).withStyle(s -> s
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("like this!")))))
			.append(Component.literal(" and <[/").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("clickable").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.AQUA).withStyle(s -> s
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("switchy-switch! ...")))
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "switchy-switch!"))))
			.append(Component.literal("]>!").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY))
		);
	}

	private static int components(String input, ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) {
		Map<Identifier, List<SwitchyComponentType<?>>> grouped = SwitchyComponentTypes.grouped(SwitchyComponentTypes.instance().values());
		long numEnabled = grouped.values().stream().filter(g -> data.componentSet().contains(g.get(0))).count();
		feedback.accept(prefix()
			.append(Component.literal("you're switching ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("%d".formatted(numEnabled).formatted(ChatFormatting.WHITE)))
			.append(Component.literal(" component%s and sharing ".formatted(numEnabled == 1 ? "" : "s")).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("%d".formatted(grouped.size() - numEnabled)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY))
		);
		grouped.forEach((id, types) -> {
			boolean enabled = data.componentSet().contains(types.get(0));
			List<SwitchyProfile> matchingProfiles = data.values().stream().sorted(Comparator.comparing(SwitchyProfile::id)).filter(p -> types.stream().anyMatch(t -> p.get(t) != null && (t.emptyChecker() == null || t.isPrecious(p.components())))).toList();
			feedback.accept(indent()
				.append(enabled ?
					clickable(Component.literal("enabled").withStyle(ChatFormatting.GREEN), "/switchy components disable %s".formatted(id), data.size() == 1 ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, Component.empty().append(Component.literal("click to share ").withStyle(ChatFormatting.GRAY)).append(id.getPath()).append(Component.literal(" between profiles.").withStyle(ChatFormatting.GRAY)).append(data.size() == 1 ? Component.empty() : Component.literal("\n").append(Component.literal("this deletes data from other profiles!").withStyle(ChatFormatting.GOLD))), ChatFormatting.RED, "[", "]") :
					clickable(Component.literal("disabled").withStyle(ChatFormatting.RED), "/switchy components enable %s".formatted(id), ClickEvent.Action.RUN_COMMAND, Component.empty().append(Component.literal("click to switch ").withStyle(ChatFormatting.GRAY)).append(id.getPath()).append(Component.literal(" per-profile.").withStyle(ChatFormatting.GRAY)), ChatFormatting.GREEN, "[", "]")
				)
				.append(" ")
				.append(id.getPath()).withStyle(s -> s
					.withColor(enabled ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)
					.withHoverEvent(!enabled ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, matchingProfiles.isEmpty() ? Component.literal("<no %s data yet>".formatted(id.getPath().replace("_", " "))).withStyle(ChatFormatting.GRAY) : ComponentUtils.formatList(matchingProfiles.stream().map(p -> Component.empty()
						.append(Component.literal(p.id()).withStyle(ChatFormatting.GRAY))
						.append(": ")
						.append(ComponentUtils.formatList(types.stream().filter(t -> p.get(t) != null).map(t -> t.asText(player.getServer(), p.components())).toList(), Component.literal(", ").withStyle(ChatFormatting.GRAY)))
					).toList(), Component.nullToEmpty("\n"))))
				)
			);
		});
		if (data.size() == 1 && data.current().equals("default")) {
			feedback.accept(indent()
				.append(Component.literal("HINT: ").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal("view the profile list via ").withStyle(ChatFormatting.GRAY))
				.append(clickable("/switchy", "/switchy", true, ChatFormatting.AQUA, "", ""))
			);
			hintClickables(feedback);
		} else if (data.size() == 1) {
			feedback.accept(indent()
				.append(Component.literal("HINT: ").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal("create your second profile via ").withStyle(ChatFormatting.GRAY))
				.append(clickable("/switchy new [name]", "/switchy new ", false, ChatFormatting.AQUA, "", ""))
			);
		}
		return data.componentSet().size();
	}

	private static int showNameFormat(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) {
		if (data.nameFormat().isPresent()) feedback.accept(prefix().append(Component.literal("import name format is currently set to:").withStyle(ChatFormatting.GRAY)));
		if (data.nameFormat().isEmpty()) feedback.accept(prefix().append(Component.literal("import name format is unset. currently defaulting to:").withStyle(ChatFormatting.GRAY)));
		feedback.accept(indent().append(FormatUtils.highlightNameFormat(data.nameFormatOrDefault())));
		return 1;
	}

	private static int changeNameFormat(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String nameFormat) {
		if (SwitchyPlayerData.NAME_FORMAT_DEFAULT.equals(nameFormat)) nameFormat = null;
		if (nameFormat != null) {
			Matcher matcher = SwitchyPlayerData.NAME_FORMAT_PATTERN.matcher(nameFormat);
			boolean found = false;
			while (matcher.find()) {
				String key = matcher.group(2);
				if (!SwitchyPlayerData.NAME_FORMAT_GETTERS.containsKey(key)) {
					feedback.accept(prefix()
						.append(Component.literal("name format placeholder '").withStyle(ChatFormatting.YELLOW))
						.append(key)
						.append(Component.literal("' is invalid!").withStyle(ChatFormatting.YELLOW))
						.append(Component.literal(" try:").withStyle(ChatFormatting.YELLOW))
					);
					feedback.accept(indent()
						.append(ComponentUtils.formatList(SwitchyPlayerData.NAME_FORMAT_GETTERS.keySet().stream().sorted().map(Component::literal).toList(), Component.literal(", ").withStyle(ChatFormatting.GRAY)))
					);
					return 0;
				} else {
					found = true;
				}
			}
			if (!found) {
				feedback.accept(prefix()
					.append(Component.literal("format doesn't contain any ").withStyle(ChatFormatting.YELLOW))
					.append(Component.literal("{{").withStyle(ChatFormatting.GRAY))
					.append(Component.literal("placeholders").withStyle(ChatFormatting.YELLOW))
					.append(Component.literal("}}").withStyle(ChatFormatting.GRAY))
					.append(Component.literal("!").withStyle(ChatFormatting.YELLOW))
					.append(Component.literal(" try:").withStyle(ChatFormatting.YELLOW))
				);
				feedback.accept(indent()
					.append(ComponentUtils.formatList(SwitchyPlayerData.NAME_FORMAT_GETTERS.keySet().stream().sorted().map(Component::literal).toList(), Component.literal(", ").withStyle(ChatFormatting.GRAY)))
				);
				return 0;
			}
		}
		data.setNameFormat(nameFormat);
		if (data.nameFormat().isPresent()) feedback.accept(prefix().append(Component.literal("import name format changed.").withStyle(ChatFormatting.GREEN)).append(Component.literal(" format is now:").withStyle(ChatFormatting.GRAY)));
		if (data.nameFormat().isEmpty()) feedback.accept(prefix().append(Component.literal("import name format reset to default.").withStyle(ChatFormatting.GREEN)).append(Component.literal(" format is now:").withStyle(ChatFormatting.GRAY)));
		feedback.accept(indent().append(FormatUtils.highlightNameFormat(data.nameFormatOrDefault())));
		return 1;
	}

	public record PlayerImportData(@Nullable String name, @Nullable String tag, @Nullable SwitchyPlayerData.Privacy privacy, List<SwitchyPlayerData.ProfileImportData> members, @Nullable List<SwitchyPlayerData.GroupImportData> groups) {}

	private static int importProfiles(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String url, String scope) {
		int beforeSize = data.size();
		PlayerImportData importData;
		try {
			importData = new Gson().fromJson(new InputStreamReader(new URL(url).openStream()), PlayerImportData.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Function<Integer, Component> feedbackGetter = updated -> prefix()
			.append(!EXISTING.equals(scope) ? Component.empty()
				.append(Component.literal("created ").withStyle(ChatFormatting.GREEN))
				.append(Component.literal("%d".formatted(data.size() - beforeSize)).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" profile%s and ".formatted((data.size() - beforeSize) == 1 ? "" : "s")).withStyle(ChatFormatting.GREEN)) : Component.empty()
			)
			.append(Component.literal("updated ").withStyle(ChatFormatting.GREEN))
			.append(Component.literal("%d".formatted(updated - (data.size() - beforeSize))).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("%s profile%s. ".formatted(!EXISTING.equals(scope) ? " existing" : "", (updated - (data.size() - beforeSize)) == 1 ? "" : "s")).withStyle(ChatFormatting.GREEN))
			.append(clickable("list", "/switchy", true));
		List<SwitchyPlayerData.ProfileImportData> profilesToImport = importData.members();
		if (!EXISTING.equals(scope) && !ALL.equals(scope)) {
			SwitchyPlayerData.GroupImportData group = Optional.ofNullable(importData.groups).orElse(new ArrayList<>()).stream().filter(g -> g.name().equals(scope)).findAny().orElse(null);
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
			int updated = data.importProfiles(new PlayerImportData(Optional.ofNullable(importData.name()).orElse(player.getGameProfile().getName()), importData.tag(), importData.privacy(), profilesToImport, importData.groups()), player, !EXISTING.equals(scope), feedbackGetter);
			feedback.accept(feedbackGetter.apply(updated));
			return data.size() - beforeSize;
		} catch (Exception e) {
			feedback.accept(prefix()
				.append("error while switching: ").withStyle(ChatFormatting.RED)
				.append(Optional.ofNullable(e.getMessage()).orElse("???")).withStyle(ChatFormatting.GRAY)
				.append(" see server logs for more info.").withStyle(ChatFormatting.RED)
			);
			Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", data.current(), player.getGameProfile().getName(), e);
			return 0;
		}
	}

	private static String quickTextInscape(String input) {
		return input.replace("\\<", "<").replace("\\'", "'");
	}

	private static int export(String input, ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) {
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
				// rip out color and name from display name
				String name = profile.get(SwitchyComponentTypes.NAME);
				String color = null;
				if (name != null) {
					Matcher colorMatcher = COLOR_PATTERN.matcher(name);
					if (colorMatcher.find()) color = quickTextInscape(colorMatcher.group(1));
					name = SwitchyComponentTypes.NAME.asText(player.getServer(), name).getString();
				}
				// pronouns
				String pronouns = null;
				try {
					SwitchyComponentType<String> pronounsComponent = (SwitchyComponentType<String>) SwitchyComponentTypes.instance().get(SwitchyComponentTypes.LAMPBLACK_PRONOUNS);
					if (pronounsComponent != null) {
						pronouns = profile.get(pronounsComponent);
					}
				} catch (ClassCastException e) {
					// pass
				}
				// bodge player renderer avatar from skin
				String avatarUrl = null;
				if (Switchy.CONFIG.exportAvatarUrl.contains("%s")) {
					String key = player.getGameProfile().getName();
					Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = player.getServer().getSessionService().getTextures(player.getGameProfile(), false);
					SwitchyComponentType<?> skinComponent = SwitchyComponentTypes.instance().get(SwitchyComponentTypes.TAILOR_SKIN);
					if (skinComponent != null && profile.contains(skinComponent) && profile.get(skinComponent) instanceof CompoundTag skinCompound && skinCompound.get("value") instanceof StringTag valueString) {
						Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
						MinecraftTexturesPayload payload = gson.fromJson(new String(Base64.getDecoder().decode(valueString.getAsString())), MinecraftTexturesPayload.class);
						textures = payload.getTextures();
					}
					MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
					if (skin != null) key = skin.getHash();
					avatarUrl = Switchy.CONFIG.exportAvatarUrl.formatted(key);
				}
				List<SwitchyPlayerData.ProxyTag> proxyTags = profile.getOrDefault(SwitchyComponentTypes.TAG, new ArrayList<SwitchyComponentTypes.Tag>()).stream().map(t -> new SwitchyPlayerData.ProxyTag(t.prefix(), t.suffix())).toList();
				members.add(new SwitchyPlayerData.ProfileImportData(null, profileId, name, color, pronouns, null, avatarUrl, proxyTags, null, components));
			}
			feedback.accept(prefix()
				.append(Component.literal("exported ").withStyle(ChatFormatting.GREEN))
				.append(Component.literal("%d".formatted(data.size())))
				.append(Component.literal(" profile%s. ".formatted(data.size() == 1 ? "" : "s")).withStyle(ChatFormatting.GREEN))
				.append(clickable("copy", SwitchyComponentTypes.GSON.toJson(new PlayerImportData(player.getGameProfile().getName(), null, null, members, null)), ClickEvent.Action.COPY_TO_CLIPBOARD, ChatFormatting.AQUA, "<", ">"))
			);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		return data.size();
	}

	private static int viewProfile(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String profileId) {
		SwitchyProfile profile;
		try {
			profile = data.getProfile(profileId, player);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		if (profile == null) {
			feedback.accept(prefix().append(Component.literal("profile doesn't exist!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		List<MutableComponent> texts = profile.components().asTexts(player.getServer());
		feedback.accept(prefix()
			.append(Component.literal("profile ").withStyle(ChatFormatting.GRAY))
			.append(profileId)
			.append(Component.literal(" contains ").withStyle(ChatFormatting.GRAY))
			.append("%d".formatted(texts.size()))
			.append(Component.literal(" component%s. ".formatted(profile.components().size() == 1 ? "" : "s")).withStyle(ChatFormatting.GRAY))
			.append(profileId.equals(data.current()) ? clickable("list", "/switchy", true) : clickable("switch", "/switch %s".formatted(StringArgumentType.escapeIfRequired(profileId)), true))
		);
		texts.forEach(componentText -> feedback.accept(indent().append(componentText)));
		if (data.size() == 1 && data.current().equals("default")) {
			hintClickables(feedback);
		}
		return profile.components().size();
	}


	private static int say(CommandContext<CommandSourceStack> context, ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String profileId) {
		SwitchyProfile profile;
		try {
			profile = data.getProfile(profileId, player);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		if (profile == null) {
			feedback.accept(prefix().append(Component.literal("profile doesn't exist!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		try {
			MessageArgument.resolveChatMessage(context, "message", message -> say(message, player, profile));
		} catch (CommandSyntaxException e) {
			throw new RuntimeException(e);
		}
		return 1;
	}

	public static void say(PlayerChatMessage message, ServerPlayer player, SwitchyProfile profile) {
		try {
			((SwitchyGameProfile) player.getGameProfile()).switchy$setSayProfile(profile);
			CommandSourceStack source = player.createCommandSourceStack(); // display name hooked here
			if (Switchy.STYLED_CHAT) StyledChatCompat.modifyForSending(message, source, ChatType.CHAT);
			source.getServer().getPlayerList().broadcastChatMessage(message, source, ChatType.bind(ChatType.CHAT, source)); // skin ID might be hooked here?
		} catch (Exception e) {
			Switchy.LOGGER.error("[Switchy] Error while performing say");
		} finally {
			((SwitchyGameProfile) player.getGameProfile()).switchy$setSayProfile(null);
		}
	}

	private static int switchProfile(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String profileId, Boolean exists) {
		String casedName = profileId;
		profileId = profileId.toLowerCase();
		boolean reallyExists = data.profileExists(profileId);
		if (data.current().equals(profileId)) {
			feedback.accept(prefix()
				.append(Component.literal("profile '").withStyle(ChatFormatting.YELLOW))
				.append(profileId)
				.append(Component.literal("' already active! specify a different profile!").withStyle(ChatFormatting.YELLOW))
			);
			return 0;
		}
		if (exists == false && reallyExists) {
			feedback.accept(prefix()
				.append("that profile already exists! try ").withStyle(ChatFormatting.YELLOW)
				.append(clickable("/switch %s".formatted(profileId), "/switch %s".formatted(profileId), true, ChatFormatting.AQUA, "", ""))
			);
			return 0;
		}
		if (exists && !reallyExists) {
			feedback.accept(prefix()
				.append(Component.literal("profile ").withStyle(ChatFormatting.YELLOW))
				.append(profileId)
				.append(Component.literal(" hasn't been made yet!").withStyle(ChatFormatting.YELLOW))
			);
			feedback.accept(indent()
				.append(Component.literal("use ").withStyle(ChatFormatting.YELLOW))
				.append(clickable("/switchy new %s".formatted(casedName), "/switchy new %s".formatted(casedName), true, ChatFormatting.AQUA, "", ""))
				.append(Component.literal(" to create it.").withStyle(ChatFormatting.YELLOW))
			);
			return 0;
		}
		try {
			SwitchyProfile currentProfile = data.getCurrentProfile(player);
			SwitchyProfile nextProfile = data.getOrCreateProfile(profileId, player);
			if (!exists) { // creation affordances
				if (!casedName.equals(profileId) && data.componentSet().contains(SwitchyComponentTypes.NAME)) nextProfile.set(SwitchyComponentTypes.NAME, casedName);
			}
			data.switchOrCreateProfile(profileId, player, prefix()
				.append(getProfileText(player, currentProfile))
				.append(Component.literal(" \uD83E\uDC46 ").withStyle(ChatFormatting.GREEN))
				.append(getProfileText(player, nextProfile))
				.append(Component.literal("! ").withStyle(ChatFormatting.GREEN))
				.append(clickable("list", "/switchy", true)));
		} catch (ProfileCurrentException e) {
			feedback.accept(prefix()
				.append(Component.literal("profile '").withStyle(ChatFormatting.YELLOW))
				.append(profileId)
				.append(Component.literal("' already active! specify a different profile!").withStyle(ChatFormatting.YELLOW))
			);
			return 0;
		} catch (Exception e) {
			feedback.accept(prefix()
				.append("error while switching: ").withStyle(ChatFormatting.RED)
				.append(Optional.ofNullable(e.getMessage()).orElse("???")).withStyle(ChatFormatting.GRAY)
				.append(" see server logs for more info.").withStyle(ChatFormatting.RED)
			);
			Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", profileId, player.getGameProfile().getName(), e);
			return 0;
		}
		return 1;
	}

	public static MutableComponent getProfileText(ServerPlayer player, SwitchyProfile profile) {
		return getProfileText(player, profile, true);
	}

	public static MutableComponent getProfileText(ServerPlayer player, SwitchyProfile profile, boolean allowBio) {
		SwitchyComponentType<?> skin = Switchy.PLACEHOLDER_API && PlaceholderApiCompat.hasHeads() ? SwitchyComponentTypes.instance().get(SwitchyComponentTypes.TAILOR_SKIN) : null;
		MutableComponent name = getNameText(player, profile);
		MutableComponent skinText = skin == null || !profile.contains(skin) ? Component.empty() : skin.asText(player.getServer(), profile.components());
		return Component.empty().append(skinText.getString().isEmpty() ? skinText : skinText.append(" ")).append(allowBio ? name : FormatUtils.stripInteraction(name));
	}

	public static MutableComponent getNameText(ServerPlayer player, SwitchyProfile profile) {
		return SwitchyComponentTypes.NAME.asText(player.getServer(), profile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id));
	}

	private static int switchNextProfile(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) {
		return switchProfile(player, data, feedback, data.profileAfter(data.current()), true);
	}

	private static int switchRandomProfile(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) {
		return switchProfile(player, data, feedback, data.randomBesides(data.current(), player.getRandom()), true);
	}

	public static <T> int editComponent(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String profileId, SwitchyComponentType<T> type, T value) {
		if (!data.componentSet().contains(type)) {
			feedback.accept(prefix().append(Component.literal("can't edit a shared component!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		SwitchyProfile profile;
		try {
			profile = data.getProfile(profileId, player);
		} catch (NbtException e) {
			throw new RuntimeException(e);
		}
		T oldValue = profile.set(type, value);
		Component feedbackText = prefix()
			.append(Component.literal("edited ").withStyle(ChatFormatting.GREEN))
			.append(profileId)
			.append(Component.literal(":").withStyle(ChatFormatting.GRAY))
			.append(type.id().getPath())
			.append(Component.literal(" - ").withStyle(ChatFormatting.GREEN))
			.append(oldValue == null ? Component.nullToEmpty("empty") : type.asText(player.getServer(), oldValue))
			.append(Component.literal(" \uD83E\uDC46 ").withStyle(ChatFormatting.GREEN))
			.append(type.asText(player.getServer(), value))
			.append(Component.literal("!").withStyle(ChatFormatting.GREEN))
			.append(" ")
			.append(clickable("list", "/switchy", true));
		if (profileId.equals(data.current()) && (type.nbtMutator() != null || type.playerMutator() != null)) {
			try {
				data.selfSwitch(profile, player, feedbackText);
				return 2;
			} catch (Exception e) {
				feedback.accept(prefix()
					.append("error while self-switching: ").withStyle(ChatFormatting.RED)
					.append(Optional.ofNullable(e.getMessage()).orElse("???")).withStyle(ChatFormatting.GRAY)
					.append(" See server logs for more info.").withStyle(ChatFormatting.RED)
				);
				Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", profileId, player.getGameProfile().getName(), e);
				return 0;
			}
		} else {
			feedback.accept(feedbackText);
			return 1;
		}
	}

	private static int renameProfile(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String profileId, String newId) {
		try {
			data.renameProfile(profileId, newId);
		} catch (IllegalArgumentException e) {
			feedback.accept(prefix().append(Component.literal(e.getMessage()).withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		feedback.accept(prefix()
			.append(Component.literal("renamed ").withStyle(ChatFormatting.GREEN))
			.append(profileId)
			.append(Component.literal(" \uD83E\uDC46 ").withStyle(ChatFormatting.GREEN))
			.append(newId)
			.append(Component.literal("!").withStyle(ChatFormatting.GREEN))
			.append(" ")
			.append(clickable("list", "/switchy", true))
		);
		return 1;
	}

	private static int deleteProfile(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, String profileId) {
		try {
			SwitchyProfile profile = data.deleteProfile(profileId);
			feedback.accept(prefix()
				.append(Component.literal("profile ").withStyle(ChatFormatting.GREEN))
				.append(profileId)
				.append(Component.literal(" deleted successfully!").withStyle(ChatFormatting.GREEN))
				.append(" ")
				.append(clickable("list", "/switchy", true))
			);
			return profile.components().size();
		} catch (ProfileCurrentException e) {
			feedback.accept(prefix().append(Component.literal("can't delete current profile!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		} catch (ProfilePreciousException e) {
			feedback.accept(prefix()
				.append(Component.literal("profile ").withStyle(ChatFormatting.YELLOW))
				.append(profileId)
				.append(Component.literal(" contains ").withStyle(ChatFormatting.YELLOW))
				.append(String.valueOf(e.getPreciousComponents().size()))
				.append(Component.literal("x precious components!").withStyle(ChatFormatting.YELLOW))
			);
			e.getPreciousComponents().asTexts(player.getServer()).forEach(componentText -> feedback.accept(indent().append(componentText)));
			return 0;
		}
	}

	private static int enableComponent(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, Identifier id) {
		Set<SwitchyComponentType<?>> types = SwitchyComponentTypes.instance().values().stream().filter(t -> id.equals(t.group())).collect(Collectors.toSet());
		if (types.isEmpty() && SwitchyComponentTypes.instance().contains(id) && SwitchyComponentTypes.instance().get(id).group() == null) types.add(SwitchyComponentTypes.instance().get(id));
		if (types.isEmpty()) {
			feedback.accept(prefix().append(Component.literal("component doesn't exist!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		if (types.stream().anyMatch(t -> data.componentSet().contains(t))) {
			feedback.accept(prefix().append(Component.literal("component is already enabled!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		int changed = data.initComponents(types, player);
		if (changed == 0) {
			feedback.accept(prefix().append(Component.literal("component failed to initialize! see logs for more info").withStyle(ChatFormatting.RED)));
			return 0;
		}
		components("", player, data, feedback);
		feedback.accept(prefix().append(Component.literal(id.getPath())).append(Component.literal(" is now switched per-profile. ").withStyle(ChatFormatting.GREEN)).append(clickable("list", "/switchy", true)));
		return changed;
	}

	private static int disableComponent(ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback, Identifier id) {
		Set<SwitchyComponentType<?>> types = SwitchyComponentTypes.instance().values().stream().filter(t -> id.equals(t.group())).collect(Collectors.toSet());
		if (types.isEmpty() && SwitchyComponentTypes.instance().contains(id) && SwitchyComponentTypes.instance().get(id).group() == null) types.add(SwitchyComponentTypes.instance().get(id));
		if (types.isEmpty()) {
			feedback.accept(prefix().append(Component.literal("component doesn't exist!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		if (types.stream().anyMatch(t -> !data.componentSet().contains(t))) {
			feedback.accept(prefix().append(Component.literal("component is already disabled!").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		int changed = data.removeComponents(types);
		if (changed == 0) {
			feedback.accept(prefix().append(Component.literal("component is precious! empty it first.").withStyle(ChatFormatting.YELLOW)));
			return 0;
		}
		components("", player, data, feedback);
		feedback.accept(prefix().append(Component.literal(id.getPath())).append(Component.literal(" is now shared between profiles. ").withStyle(ChatFormatting.GREEN)).append(clickable("list", "/switchy", true)));
		return changed;
	}

	private static final List<String> NAME_FORMATS = List.of(
		SwitchyPlayerData.NAME_FORMAT_DEFAULT,
		"<hover:'{{pronouns} | }{{system}}{ | {bio}}'>{<#{color}>}{{slug}}",
		"<hover:'{{paren} | }{{system}}{ | {bio}}'>{<#{color}>}{{shortdn}}",
		"{<#{color}>}{{name}} {{tag}}"
	);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registries, Commands.CommandSelection environment) {
		RequiredArgumentBuilder<CommandSourceStack, String> editBuilder = profile(true);
		for (SwitchyComponentType<?> type : SwitchyComponentTypes.getStatic().values()) {
			type.tryCreateEditor(arg -> editBuilder.then(Commands.literal(type.id().toString().replace("switchy:", "")).then(arg)));
		}

		dispatcher.register(
			Commands.literal("switch")
				.requires(c -> c.getPlayer() != null && SwitchyPlayerData.ofEarly(c.getPlayer()) != null && SwitchyPlayerData.ofEarly(c.getPlayer()).size() > 1)
				.then(Commands.literal("?")
					.requires(c -> c.getPlayer() != null && SwitchyPlayerData.ofEarly(c.getPlayer()) != null && SwitchyPlayerData.ofEarly(c.getPlayer()).size() > 2)
					.executes(c -> execute(c, (i, p, d, f) -> switchRandomProfile(p, d, f)))
				)
				.then(profile(false)
					.executes(c -> execute(c, (i, p, d, f) -> switchProfile(p, d, f, c.getArgument("profile", String.class), true)))
				)
				.executes(c -> execute(c, (i, p, d, f) -> switchNextProfile(p, d, f)))
		);
		dispatcher.register(
			Commands.literal("switchy")
				.then(Commands.literal("new")
					.then(Commands.argument("name", StringArgumentType.string())
						.executes(c -> execute(c, (i, p, d, f) -> switchProfile(p, d, f, c.getArgument("name", String.class), false)))
					)
				)
				.then(Commands.literal("view")
					.then(profile(true)
						.executes(c -> execute(c, (i, p, d, f) -> viewProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase())))
					)
				)
				.then(Commands.literal("say")
					.requires(c -> c.getPlayer() != null && SwitchyPlayerData.ofEarly(c.getPlayer()) != null && SwitchyPlayerData.ofEarly(c.getPlayer()).size() > 1)
					.then(profile(true)
						.then(Commands.argument("message", MessageArgument.message())
							.executes(c -> execute(c, (i, p, d, f) -> say(c, p, d, f, c.getArgument("profile", String.class).toLowerCase())))
						)
					)
				)
				.then(Commands.literal("delete")
					.requires(c -> c.getPlayer() != null && SwitchyPlayerData.ofEarly(c.getPlayer()) != null && SwitchyPlayerData.ofEarly(c.getPlayer()).size() > 1)
					.then(profile(false)
						.executes(c -> execute(c, (i, p, d, f) -> deleteProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase())))
					)
				)
				.then(Commands.literal("edit")
					.then(editBuilder
						.then(Commands.literal("id")
							.then(
								Commands.argument("id", StringArgumentType.word()).executes(c -> execute(c, (i, p, d, f) -> renameProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase(), c.getArgument("id", String.class).toLowerCase()))))
						)
					)
				)
				.then(Commands.literal("import")
					.then(Commands.literal("format")
						.then(Commands.argument("name_format", StringArgumentType.greedyString())
							.suggests((c, b) -> SharedSuggestionProvider.suggest(NAME_FORMATS, b))
							.executes(c -> execute(c, (i, p, d, f) -> changeNameFormat(p, d, f, c.getArgument("name_format", String.class))))
						)
						.executes(c -> execute(c, (i, p, d, f) -> showNameFormat(p, d, f)))
					)
					.then(Commands.argument("scope", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(List.of(ALL, EXISTING), b))
						.then(Commands.argument("url", StringArgumentType.greedyString())
							.executes(c -> execute(c, (i, p, d, f) -> importProfiles(p, d, f, c.getArgument("url", String.class), c.getArgument("scope", String.class))))
						)
					)
				)
				.then(Commands.literal("export")
					.requires(c -> c.getPlayer() != null && SwitchyPlayerData.ofEarly(c.getPlayer()) != null && SwitchyPlayerData.ofEarly(c.getPlayer()).size() > 1)
					.executes(c -> execute(c, SwitchyCommands::export))
				)
				.then(Commands.literal("components")
					.then(Commands.literal("disable")
						.then(groupedComponent(true)
							.executes(c -> execute(c, (i, p, d, f) -> disableComponent(p, d, f, c.getArgument("component", Identifier.class))))
						)
					)
					.then(Commands.literal("enable")
						.then(groupedComponent(false)
							.executes(c -> execute(c, (i, p, d, f) -> enableComponent(p, d, f, c.getArgument("component", Identifier.class))))
						)
					)
					.executes(c -> execute(c, SwitchyCommands::components))
				)
				.executes(c -> execute(c, SwitchyCommands::list))
		);
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> profile(boolean includeCurrent) {
		return Commands.argument("profile", StringArgumentType.string()).suggests((c, b) -> SharedSuggestionProvider.suggest(
			(Iterable<String>) map(c, (i, p, d, f) -> (includeCurrent ? d.keySet() : Sets.difference(d.keySet(), Set.of(d.current()))).stream().map(StringArgumentType::escapeIfRequired).toList(), false), b));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Identifier> groupedComponent(Boolean enabled) {
		return Commands.argument("component", IdentifierArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(
			(Iterable<Identifier>) map(c, (i, p, d, f) -> SwitchyComponentTypes.instance().values().stream().filter(t -> enabled == null || (!enabled ^ d.componentSet().contains(t))).map(t -> Optional.ofNullable(t.group()).orElse(t.id())).distinct().toList(), false), b));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Identifier> component(Boolean enabled) {
		return Commands.argument("component", IdentifierArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(
			(Iterable<Identifier>) map(c, (i, p, d, f) -> SwitchyComponentTypes.instance().values().stream().filter(t -> enabled == null || (!enabled ^ d.componentSet().contains(t))).map(TypeRegistry.Type::id).toList(), false), b));
	}

	public static MutableComponent prefix() {
		return Component.empty().append(Component.literal("[Switchy] ").withStyle(ChatFormatting.DARK_PURPLE));
	}

	public static MutableComponent indent() {
		return Component.empty().append(Component.literal("|| ").withStyle(ChatFormatting.DARK_PURPLE));
	}

	public static MutableComponent clickable(String name, String contents, ClickEvent.Action action, ChatFormatting formatting, String prefix, String suffix) {
		return clickable(Component.literal(name).withStyle(formatting), contents, action, null, formatting, prefix, suffix);
	}

	public static MutableComponent clickable(Component name, String contents, ClickEvent.Action action, Component hint, ChatFormatting formatting, String prefix, String suffix) {
		return Component.empty()
			.append(Component.literal(prefix).withStyle(ChatFormatting.GRAY))
			.append(name.copy().withStyle(s -> s
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.empty().append(hint == null ? Component.empty() : hint.copy().append("\n")).append(Component.literal(contents + (action == ClickEvent.Action.SUGGEST_COMMAND ? "..." : "")).withStyle(formatting))))
				.withClickEvent(new ClickEvent(action, contents))
			))
			.append(Component.literal(suffix).withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent clickable(String name, String contents, boolean instant, ChatFormatting formatting, String prefix, String suffix) {
		return clickable(name, contents, instant ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, formatting, prefix, suffix);
	}

	public static MutableComponent clickable(String name, String command, boolean instant) {
		return clickable(name, command, instant, ChatFormatting.AQUA, "<", ">");
	}

	public static <T> T map(CommandContext<CommandSourceStack> context, SurveyorCommandExecutor<T> executor, boolean feedback) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (CommandSyntaxException e) {
			if (feedback) Switchy.LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return null;
		}

		SwitchyPlayerData data = SwitchyPlayerData.of(player);
		try {
			return executor.execute(context.getInput(), player, data, t -> context.getSource().sendSuccess(() -> t, false));
		} catch (ProfileMissingException e) {
			context.getSource().sendSuccess(() -> prefix().append(Component.literal("profile doesn't exist!").withStyle(ChatFormatting.YELLOW)), false);
			return null;
		} catch (Exception e) {
			if (feedback) context.getSource().sendSuccess(() -> prefix().append(Component.literal("command \"/%s...\" failed! Check log for details.".formatted(context.getInput().substring(0, Math.min(context.getInput().length(), 20)))).withStyle(ChatFormatting.RED)), false);
			if (feedback) Switchy.LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return null;
		}
	}

	public static int execute(CommandContext<CommandSourceStack> context, SurveyorCommandExecutor<Integer> executor) {
		return Objects.requireNonNullElse(map(context, executor, true), 0);
	}

    public interface SurveyorCommandExecutor<T> {
		T execute(String input, ServerPlayer player, SwitchyPlayerData data, Consumer<Component> feedback) throws ProfileMissingException;
	}
}

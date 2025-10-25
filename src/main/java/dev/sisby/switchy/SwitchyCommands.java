package dev.sisby.switchy;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.exception.ProfileCurrentException;
import dev.sisby.switchy.exception.ProfileMissingException;
import dev.sisby.switchy.exception.ProfilePreciousException;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
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
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SwitchyCommands {
	public static void greet(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		SwitchyPlayerData data = SwitchyPlayerData.of(handler.getPlayer());
		if (!data.previous().isEmpty()) {
			handler.getPlayer().sendMessage(prefix()
				.append(Text.literal("Switched from ").formatted(Formatting.GREEN))
				.append(SwitchyComponentTypes.NAME.asText(data.getProfile(data.previous()).getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)))
				.append(Text.literal(" to ").formatted(Formatting.GREEN))
				.append(SwitchyComponentTypes.NAME.asText(data.getCurrentProfile().getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)))
				.append(Text.literal("! ").formatted(Formatting.GREEN))
				.append(clickable("list", "/switchy", true))
			);
			data.clearPrevious();
		} else if (data.size() > 1) {
			handler.getPlayer().sendMessage(prefix()
				.append(Text.literal("welcome back! current profile: ").formatted(Formatting.GRAY))
				.append(SwitchyComponentTypes.NAME.asText(data.getCurrentProfile().getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id)))
				.append(Text.literal(". ").formatted(Formatting.GRAY))
				.append(clickable("list", "/switchy", true))
			);
		}
	}

	private static int list(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		try {
			data.updateCurrent(player);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		feedback.accept(prefix()
			.append(Text.literal("you have ").formatted(Formatting.GRAY))
			.append(Text.literal("%s".formatted(data.size())).formatted(Formatting.WHITE))
			.append(Text.literal(" profiles available. ").formatted(Formatting.GRAY))
			.append(clickable("new", "/switchy switch ", false))
		);
		for (SwitchyProfile profile : Stream.concat(Sets.difference(data.keySet(), Set.of(data.current())).stream().sorted(), Stream.of(data.current())).map(data::getProfile).toList()) {
			feedback.accept(indent()
				.append(profile.id().equals(data.current()) ? Text.literal("current").formatted(Formatting.GRAY) : clickable("switch", "/switchy switch %s".formatted(profile.id()), true))
				.append(" ")
				.append(clickable("view", "/switchy view %s".formatted(profile.id()), true))
				.append(" ")
				.append(clickable("edit", "/switchy edit %s ".formatted(profile.id()), false))
				.append(" ")
				.append(SwitchyComponentTypes.NAME.asText(profile.getOrGetDefault(SwitchyComponentTypes.NAME, SwitchyProfile::id))).setStyle(Style.EMPTY
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Texts.join(profile.asTexts(player), Text.of("\n"))))
				)
			);
		}
		return data.size();
	}

	public record PlayerImportData(@Nullable String name, List<SwitchyPlayerData.ProfileImportData> members) {}

	private static int importProfiles(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String url) {
		int beforeSize = data.size();
		PlayerImportData importData;
		try {
			importData = new Gson().fromJson(new InputStreamReader(new URL(url).openStream()), PlayerImportData.class);
			data.importProfiles(importData.members(), player, importData.name());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		feedback.accept(prefix()
			.append(Text.literal("imported ").formatted(Formatting.GRAY))
			.append(Text.literal("%s".formatted(data.size() - beforeSize)).formatted(Formatting.WHITE))
			.append(Text.literal(" new and updated ").formatted(Formatting.GRAY))
			.append(Text.literal("%s".formatted(importData.members().size() - (data.size() - beforeSize))).formatted(Formatting.WHITE))
			.append(Text.literal(" existing profiles.").formatted(Formatting.GRAY))
			.append(clickable("list", "/switchy", true))
		);
		return data.size();
	}


	private static int viewProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId) {
		try {
			if (profileId.equals(data.current())) data.updateCurrent(player);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		SwitchyProfile profile = data.getProfile(profileId);
		if (profile == null) {
			feedback.accept(prefix().append(Text.literal("profile doesn't exist!").formatted(Formatting.YELLOW)));
			return 0;
		}
		feedback.accept(prefix()
			.append(Text.literal("profile ").formatted(Formatting.GRAY))
			.append(profileId)
			.append(Text.literal(" contains ").formatted(Formatting.GRAY))
			.append("%d".formatted(profile.components().size()))
			.append(Text.literal(" components. ").formatted(Formatting.GRAY))
			.append(profileId.equals(data.current()) ? Text.empty() : clickable("switch", "/switchy switch %s".formatted(profileId), true))
		);
		profile.components().asTexts().forEach(componentText -> feedback.accept(indent().append(componentText)));
		return profile.components().size();
	}

	private static int switchProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId) {
		SwitchyProfile currentProfile = data.getCurrentProfile();
		SwitchyProfile nextProfile;
		try {
			nextProfile = data.switchOrCreateProfile(profileId, player);
		} catch (Exception e) {
			feedback.accept(prefix()
				.append("Error while switching: ").formatted(Formatting.RED)
				.append(Objects.requireNonNullElse(e.getMessage(), "???")).formatted(Formatting.GRAY)
				.append(" See server logs for more info.").formatted(Formatting.RED)
			);
			Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", profileId, player.getGameProfile().getName(), e);
			return 0;
		}
		return 1;
	}

	public static <T> int editComponent(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId, SwitchyComponentType<T> type, T value) {
		SwitchyProfile profile = data.getProfile(profileId);
		T oldValue = profile.set(type, value);
		feedback.accept(prefix()
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
			.append(clickable("list", "/switchy", true))
		);
		return 1;
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
		} catch (ProfileMissingException e) {
			feedback.accept(prefix().append(Text.literal("profile doesn't exist!").formatted(Formatting.YELLOW)));
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

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registries, CommandManager.RegistrationEnvironment environment) {
		RequiredArgumentBuilder<ServerCommandSource, String> editBuilder = profile(true);
		for (SwitchyComponentType<?> type : SwitchyComponentTypes.instance().values()) {
			type.tryCreateEditor(arg -> editBuilder.then(CommandManager.literal(type.id().toString().replace("switchy:", "")).then(arg)));
		}

		dispatcher.register(
			CommandManager.literal("switchy")
				.then(CommandManager.literal("switch")
					.then(profile(false)
						.executes(c -> execute(c, (i, p, d, f) -> switchProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase())))
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
					.then(CommandManager.argument("url", StringArgumentType.greedyString())
						.executes(c -> execute(c, (i, p, d, f) -> importProfiles(p, d, f, c.getArgument("url", String.class))))
					)
				)
				.executes(c -> execute(c, SwitchyCommands::list))
		);
	}

	private static RequiredArgumentBuilder<ServerCommandSource, String> profile(boolean includeCurrent) {
		return CommandManager.argument("profile", StringArgumentType.word()).suggests((c, b) -> CommandSource.suggestMatching(
			(Iterable<String>) map(c, (i, p, d, f) -> includeCurrent ? d.keySet() : Sets.difference(d.keySet(), Set.of(d.current())) , false), b));
	}

	public static MutableText prefix() {
		return Text.empty().append(Text.literal("[Switchy] ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText indent() {
		return Text.empty().append(Text.literal("|| ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText clickable(String name, String command, boolean instant) {
		return Text.empty()
			.append(Text.literal("<").formatted(Formatting.GRAY))
			.append(Text.literal(name).setStyle(Style.EMPTY
				.withFormatting(Formatting.AQUA)
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command + (instant ? "" : "...")).formatted(Formatting.AQUA)))
				.withClickEvent(new ClickEvent(instant ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command))
			))
			.append(Text.literal(">").formatted(Formatting.GRAY));
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
		} catch (Exception e) {
			if (feedback) context.getSource().sendFeedback(() -> prefix().append(Text.literal("Command \"/%s...\" failed! Check log for details.".formatted(context.getInput().substring(0, Math.min(context.getInput().length(), 20)))).formatted(Formatting.RED)), false);
			if (feedback) Switchy.LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return null;
		}
	}

	public static int execute(CommandContext<ServerCommandSource> context, SurveyorCommandExecutor<Integer> executor) {
		return Objects.requireNonNullElse(map(context, executor, true), 0);
	}

    public interface SurveyorCommandExecutor<T> {
		T execute(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback);
	}
}

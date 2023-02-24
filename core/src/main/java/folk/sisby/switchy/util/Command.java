package folk.sisby.switchy.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.util.Feedback.tellInvalid;

public class Command {
	public interface SwitchyServerCommandExecutor { void execute(ServerPlayerEntity player, SwitchyPresets presets); }

	public static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean allowCurrent) throws CommandSyntaxException {
		SwitchyPresets presets = ((SwitchyPlayer) context.getSource().getPlayer()).switchy$getPresets();
		CommandSource.suggestMatching(presets.getPresetNames().stream().filter((s) -> allowCurrent || !Objects.equals(s, presets.getCurrentPresetName())), builder);
		return builder.buildFuture();
	}

	public static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean enabled) throws CommandSyntaxException {
		SwitchyPresets presets = ((SwitchyPlayer) context.getSource().getPlayer()).switchy$getPresets();
		CommandSource.suggestIdentifiers(presets.getModules().entrySet().stream().filter(e -> e.getValue() == enabled).map(Map.Entry::getKey), builder);
		return builder.buildFuture();
	}

	public static @Nullable ServerPlayerEntity serverPlayerOrNull(ServerCommandSource source) {
		try {
			return source.getPlayer();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	public static <EventType> void consumeEventPacket(PacketByteBuf buf, Function<NbtCompound, EventType> parser, Consumer<EventType> eventHandler) {
		NbtCompound eventNbt = buf.readNbt();
		if (eventNbt != null) {
			eventHandler.accept(parser.apply(eventNbt));
		}
	}

	public static <T> T unwrap(CommandContext<? extends CommandSource> context, String argument, Class<T> argumentClass) {
		return context.getArgument(argument, argumentClass);
	}

	public static int execute(CommandContext<ServerCommandSource> context, SwitchyServerCommandExecutor executor) {
		ServerPlayerEntity player = serverPlayerOrNull(context.getSource());
		if (player == null) {
			LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return 0;
		}

		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		try {
			executor.execute(player, presets);
			return 1;
		} catch (Exception e) {
			tellInvalid(player, "commands.switchy.fail");
			LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return 0;
		}
	}
}

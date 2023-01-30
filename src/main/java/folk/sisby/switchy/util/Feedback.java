package folk.sisby.switchy.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.Arrays;
import java.util.List;

public class Feedback {
	public static final Pair<Style, Style> FORMAT_SUCCESS = new Pair<>(Style.EMPTY.withColor(Formatting.GREEN), Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	public static final Pair<Style, Style> FORMAT_INVALID = new Pair<>(Style.EMPTY.withColor(Formatting.YELLOW), Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	public static final Pair<Style, Style> FORMAT_INFO = new Pair<>(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), Style.EMPTY.withColor(Formatting.WHITE));
	public static final Pair<Style, Style> FORMAT_WARN = new Pair<>(Style.EMPTY.withColor(Formatting.GOLD), Style.EMPTY.withColor(Formatting.GRAY));
	public static final Pair<Style, Style> FORMAT_COMMAND = new Pair<>(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
	public static final Pair<Style, Style> FORMAT_HELP = new Pair<>(Style.EMPTY.withColor(Formatting.WHITE), Style.EMPTY.withColor(Formatting.WHITE));


	public static void sendMessage(ServerPlayerEntity player, Text text) {
		player.sendMessage(literal("[Switchy] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	public static void sendClientMessage(ClientPlayerEntity player, Text text) {
		player.sendMessage(literal("[Switchy Client] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	public static void tellHelp(ServerPlayerEntity player, String keyHelp, String keyCommand, String... keyArgs) {
		sendMessage(player, translatableWithArgs("commands.switchy.help.line", translatableWithArgs(keyCommand, FORMAT_COMMAND, translatable(keyArgs)), translatableWithArgs(keyHelp, FORMAT_HELP)));
	}

	public static void tellInvalidTry(ServerPlayerEntity player, String keyFail, String keyCommand, MutableText... commandArgs) {
		sendMessage(player, translatableWithArgs(keyFail, FORMAT_INVALID, translatableWithArgs(keyCommand, commandArgs)));
	}

	public static void tellInvalidTry(ClientPlayerEntity player, String keyFail, String keyCommand, MutableText... commandArgs) {
		sendClientMessage(player, translatableWithArgs(keyFail, FORMAT_INVALID, translatableWithArgs(keyCommand, commandArgs)));
	}

	public static void tellInvalid(ServerPlayerEntity player, String key, MutableText... args) {
		sendMessage(player, translatableWithArgs(key, FORMAT_INVALID, args));
	}

	public static void tellInvalid(ClientPlayerEntity player, String key, MutableText... args) {
		sendClientMessage(player, translatableWithArgs(key, FORMAT_INVALID, args));
	}

	public static void tellSuccess(ServerPlayerEntity player, String key, MutableText... args) {
		sendMessage(player, translatableWithArgs(key, FORMAT_SUCCESS, args));
	}

	public static void tellSuccess(ClientPlayerEntity player, String key, MutableText... args) {
		sendClientMessage(player, translatableWithArgs(key, FORMAT_SUCCESS, args));
	}

	public static void tellWarn(ServerPlayerEntity player, String key, MutableText... args) {
		sendMessage(player, translatableWithArgs(key, FORMAT_WARN, args));
	}

	public static void tellWarn(ClientPlayerEntity player, String key, MutableText... args) {
		sendClientMessage(player, translatableWithArgs(key, FORMAT_WARN, args));
	}

	public static MutableText translatable(String key) {
		return translatableWithArgs(key);
	}

	public static MutableText[] translatable(String... keys) {
		return Arrays.stream(keys).map(Feedback::translatable).toArray(MutableText[]::new);
	}

	public static MutableText translatableWithArgs(String key, Pair<Style, Style> formatStyle, MutableText... args) {
		return translatableWithArgs(key, Arrays.stream(args).map(text -> (text.setStyle(formatStyle.getRight()))).toArray(MutableText[]::new)).setStyle(formatStyle.getLeft());
	}

	public static MutableText translatableWithArgs(String key, MutableText... args) {
		return new TranslatableText(key, (Object[]) args);
	}

	public static MutableText literal(String string) {
		return new LiteralText(string);
	}

	public static MutableText getIdText(List<Identifier> identifiers) {
		return literal("[").append(Texts.join(
				identifiers,
				literal(", "),
				id -> literal(id.getPath()).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, literal(id.toString()))))
		)).append(literal("]"));
	}

	public static String command(String string) { return "/" + string; }
}

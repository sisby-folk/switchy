package folk.sisby.switchy.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Sisby folk
 * @since 1.6.1
 * Utilities for styling text and sending translatable feedback
 */
public class Feedback {
	/**
	 * Green text with white italic arguments
	 */
	public static final Pair<Style, Style> FORMAT_SUCCESS = new Pair<>(Style.EMPTY.withColor(Formatting.GREEN), Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	/**
	 * Yellow text with white italic arguments
	 */
	public static final Pair<Style, Style> FORMAT_INVALID = new Pair<>(Style.EMPTY.withColor(Formatting.YELLOW), Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	/**
	 * Grey italic text with white arguments
	 */
	public static final Pair<Style, Style> FORMAT_INFO = new Pair<>(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), Style.EMPTY.withColor(Formatting.WHITE));
	/**
	 * Gold text with grey arguments
	 */
	public static final Pair<Style, Style> FORMAT_WARN = new Pair<>(Style.EMPTY.withColor(Formatting.GOLD), Style.EMPTY.withColor(Formatting.GRAY));
	/**
	 * Grey italic text with grey italic arguments
	 */
	public static final Pair<Style, Style> FORMAT_COMMAND = new Pair<>(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
	/**
	 * White text with white arguments
	 */
	public static final Pair<Style, Style> FORMAT_HELP = new Pair<>(Style.EMPTY.withColor(Formatting.WHITE), Style.EMPTY.withColor(Formatting.WHITE));

	/**
	 * @param player the player to send the message to
	 * @param text the text to send
	 */
	public static void sendMessage(ServerPlayerEntity player, Text text) {
		player.sendMessage(literal("[Switchy] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	/**
	 * @param player the player to send the message to
	 * @param keyHelp a translation key explaining the command
	 * @param keyCommand a translation key with a command
	 * @param keyArgs the translation key arguments to pass to the command text
	 * Sends feedback to the player with a suggested command, using the {@link Feedback#FORMAT_INVALID} style
	 */
	public static void tellHelp(ServerPlayerEntity player, String keyHelp, String keyCommand, String... keyArgs) {
		sendMessage(player, translatableWithArgs("commands.switchy.help.line", translatableWithArgs(keyCommand, FORMAT_COMMAND, translatable(keyArgs)), translatableWithArgs(keyHelp, FORMAT_HELP)));
	}

	/**
	 * @param player the player to send the message to
	 * @param keyFail a translation key explaining why the user action failed
	 * @param keyCommand a translation key with a suggested command
	 * @param commandArgs the literal arguments to pass to the command text
	 * Sends feedback to the player with a suggested command, using the {@link Feedback#FORMAT_INVALID} style
	 */
	public static void tellInvalidTry(ServerPlayerEntity player, String keyFail, String keyCommand, MutableText... commandArgs) {
		sendMessage(player, translatableWithArgs(keyFail, FORMAT_INVALID, translatableWithArgs(keyCommand, commandArgs)));
	}

	/**
	 * @param player the player to send the message to
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * Sends feedback to the player using the {@link Feedback#FORMAT_INVALID} style
	 */
	public static void tellInvalid(ServerPlayerEntity player, String key, MutableText... args) {
		sendMessage(player, translatableWithArgs(key, FORMAT_INVALID, args));
	}


	/**
	 * @param player the player to send the message to
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * Sends feedback to the player using the {@link Feedback#FORMAT_SUCCESS} style
	 */
	public static void tellSuccess(ServerPlayerEntity player, String key, MutableText... args) {
		sendMessage(player, translatableWithArgs(key, FORMAT_SUCCESS, args));
	}

	/**
	 * @param player the player to send the message to
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * Sends feedback to the player using the {@link Feedback#FORMAT_WARN} style
	 */
	public static void tellWarn(ServerPlayerEntity player, String key, MutableText... args) {
		sendMessage(player, translatableWithArgs(key, FORMAT_WARN, args));
	}

	/**
	 * @param key a translation key
	 * @return the resultant translatable text
	 */
	public static MutableText translatable(String key) {
		return translatableWithArgs(key);
	}

	/**
	 * @param keys an arbitrary amount of translation keys
	 * @return an array of the resultant translatable text
	 */
	public static MutableText[] translatable(String... keys) {
		return Arrays.stream(keys).map(Feedback::translatable).toArray(MutableText[]::new);
	}

	/**
	 * @param key a translation key
	 * @param formatStyle a pair of styles - left applies to the translatable text, right to the arguments.
	 * @param args the literal arguments to pass to the translatable text
	 * @return the resultant translatable text
	 */
	public static MutableText translatableWithArgs(String key, Pair<Style, Style> formatStyle, MutableText... args) {
		return translatableWithArgs(key, Arrays.stream(args).map(text -> (text.setStyle(formatStyle.getRight()))).toArray(MutableText[]::new)).setStyle(formatStyle.getLeft());
	}

	/**
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * @return the resultant translatable text
	 */
	public static MutableText translatableWithArgs(String key, MutableText... args) {
		return Text.translatable(key, (Object[]) args);
	}

	/**
	 * @param string an arbitrary string
	 * @return A literal text instance of the string
	 */
	public static MutableText literal(String string) {
		return Text.literal(string);
	}

	/**
	 * @param identifiers a list of IDs
	 * @return a text representation of the list, with only ID paths shown, but with full IDs under tooltip.
	 */
	public static MutableText getIdListText(List<Identifier> identifiers) {
		return literal("[").append(Texts.join(
				identifiers,
				literal(", "),
				id -> literal(id.getPath()).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, literal(id.toString()))))
		)).append(literal("]"));
	}

	/**
	 * @param list an arbitrary list of strings
	 * @param highlighter a list of "highlighters" - formatting with predicates to apply to the string before applying.
	 * @return a text representation of the list with formatting applied to the strings based on the highlighters.
	 * Highlighters are first-in first-serve.
	 */
	public static MutableText getHighlightedListText(List<String> list, List<Pair<Predicate<String>, Formatting>> highlighter) {
		return literal("[").append(Texts.join(
				list,
				literal(", "),
				str -> literal(str).setStyle(Style.EMPTY.withFormatting(highlighter.stream().filter(e -> e.getLeft().test(str)).map(Pair::getRight).findFirst().orElse(Formatting.RESET)))
		)).append(literal("]"));
	}

	/**
	 * @param string a command string without a slash.
	 * @return a command as might be returned by context.getInput(). Identical in 1.19+
	 */
	public static String command(String string) { return "" + string; }
}

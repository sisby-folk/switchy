package folk.sisby.switchy.util;

import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.text.component.LiteralComponent;
import net.minecraft.text.component.TranslatableComponent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utilities for styling text and sending translatable feedback.
 *
 * @author Sisby folk
 * @since 1.6.1
 */
@SuppressWarnings("deprecation")
public class Feedback {
	/**
	 * Green text with white italic arguments.
	 */
	public static final Pair<Style, Style> FORMAT_SUCCESS = new Pair<>(Style.EMPTY.withColor(Formatting.GREEN), Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	/**
	 * Yellow text with white italic arguments.
	 */
	public static final Pair<Style, Style> FORMAT_INVALID = new Pair<>(Style.EMPTY.withColor(Formatting.YELLOW), Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	/**
	 * Grey italic text with white arguments.
	 */
	public static final Pair<Style, Style> FORMAT_INFO = new Pair<>(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), Style.EMPTY.withColor(Formatting.WHITE));
	/**
	 * Gold text with grey arguments.
	 */
	public static final Pair<Style, Style> FORMAT_WARN = new Pair<>(Style.EMPTY.withColor(Formatting.GOLD), Style.EMPTY.withColor(Formatting.GRAY));
	/**
	 * Grey italic text with grey italic arguments.
	 */
	public static final Pair<Style, Style> FORMAT_COMMAND = new Pair<>(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
	/**
	 * White text with white arguments.
	 */
	public static final Pair<Style, Style> FORMAT_HELP = new Pair<>(Style.EMPTY.withColor(Formatting.WHITE), Style.EMPTY.withColor(Formatting.WHITE));

	/**
	 * Sends feedback to the player.
	 *
	 * @param player the player to send the message to.
	 * @param text   the text to send.
	 */
	public static void sendMessage(ServerPlayerEntity player, Text text) {
		player.sendMessage(literal("[Switchy] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	/**
	 * Creates translatable text from a key.
	 *
	 * @param key a translation key.
	 * @return the resultant translatable text.
	 */
	public static MutableText translatable(String key) {
		return MutableText.create(new TranslatableComponent(key, null, new Object[0]));
	}

	/**
	 * Creates translatable text from a key with arguments.
	 *
	 * @param key  a translation key.
	 * @param args the literal arguments to pass to the translatable text.
	 * @return the resultant translatable text.
	 */
	public static MutableText translatable(String key, Object... args) {
		return MutableText.create(new TranslatableComponent(key, null, args));
	}

	/**
	 * Creates dual-coloured translatable text from a key with arguments.
	 *
	 * @param key         a translation key.
	 * @param formatStyle a pair of styles - left applies to the translatable text, right to the arguments.
	 * @param args        the literal arguments to pass to the translatable text.
	 * @return the resultant translatable text.
	 */
	public static MutableText translatableWithStyle(String key, Pair<Style, Style> formatStyle, MutableText... args) {
		return translatable(key, Arrays.stream(args).map(text -> (text.setStyle(formatStyle.getRight()))).toArray(Object[]::new)).setStyle(formatStyle.getLeft());
	}

	/**
	 * Creates literal text from a string.
	 *
	 * @param string an arbitrary string.
	 * @return a literal text instance of the string.
	 */
	public static MutableText literal(String string) {
		return MutableText.create(new LiteralComponent(string));
	}

	/**
	 * Creates text for a compact representation of a list of identifiers.
	 *
	 * @param identifiers a list of IDs.
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
	 * Creates text for a representation of a list with conditional element styling.
	 *
	 * @param list        an arbitrary list of strings.
	 * @param highlighter a list of "highlighters" - formatting with predicates to apply to the string before applying.
	 *                    Highlighters are first-in first-serve.
	 * @return a text representation of the list with formatting applied to the strings based on the highlighters.
	 */
	public static MutableText getHighlightedListText(List<String> list, List<Pair<Predicate<String>, Formatting>> highlighter) {
		return literal("[").append(Texts.join(
				list,
				literal(", "),
				str -> literal(str).setStyle(Style.EMPTY.withFormatting(highlighter.stream().filter(e -> e.getLeft().test(str)).map(Pair::getRight).findFirst().orElse(Formatting.RESET)))
		)).append(literal("]"));
	}

	/**
	 * Formats a slash-less command to match {@code context.getInput()}.
	 *
	 * @param string a command string without a slash.
	 * @return a command as might be returned by context.getInput(). Identical in 1.19+.
	 */
	public static String command(String string) {
		return string;
	}

	/**
	 * Gets formatted text using the {@link Feedback#FORMAT_INFO} style.
	 *
	 * @param key  a translation key.
	 * @param args the literal arguments to pass to the translatable text.
	 * @return the resultant translatable text.
	 */
	public static MutableText info(String key, MutableText... args) {
		return translatableWithStyle(key, FORMAT_INFO, args);
	}

	/**
	 * Gets formatted text explaining a command using the {@link Feedback#FORMAT_HELP} style.
	 *
	 * @param keyHelp    a translation key explaining the command.
	 * @param keyCommand a translation key with a command.
	 * @param keyArgs    the translation key arguments to pass to the command text.
	 * @return the resultant translatable text.
	 */
	public static MutableText helpText(String keyHelp, String keyCommand, String... keyArgs) {
		return translatable("commands.switchy.help.line", translatableWithStyle(keyCommand, FORMAT_COMMAND, Arrays.stream(keyArgs).map(Feedback::translatable).toArray(MutableText[]::new)), translatableWithStyle(keyHelp, FORMAT_HELP));
	}

	/**
	 * Gets formatted text with a suggested command, using the {@link Feedback#FORMAT_INVALID} style.
	 *
	 * @param keyFail     a translation key explaining why the user action failed.
	 * @param keyCommand  a translation key with a suggested command.
	 * @param commandArgs the literal arguments to pass to the command text.
	 * @return the resultant translatable text.
	 */
	public static MutableText invalidTry(String keyFail, String keyCommand, MutableText... commandArgs) {
		return translatableWithStyle(keyFail, FORMAT_INVALID, translatable(keyCommand, (Object[]) commandArgs));
	}

	/**
	 * Gets formatted text using the {@link Feedback#FORMAT_INVALID} style.
	 *
	 * @param key  a translation key.
	 * @param args the literal arguments to pass to the translatable text.
	 * @return the resultant translatable text.
	 */
	public static MutableText invalid(String key, MutableText... args) {
		return translatableWithStyle(key, FORMAT_INVALID, args);
	}


	/**
	 * Gets formatted text using the {@link Feedback#FORMAT_SUCCESS} style.
	 *
	 * @param key  a translation key.
	 * @param args the literal arguments to pass to the translatable text.
	 * @return the resultant translatable text.
	 */
	public static MutableText success(String key, MutableText... args) {
		return translatableWithStyle(key, FORMAT_SUCCESS, args);
	}

	/**
	 * Gets formatted text using the {@link Feedback#FORMAT_WARN} style.
	 *
	 * @param key  a translation key.
	 * @param args the literal arguments to pass to the translatable text.
	 * @return the resultant translatable text.
	 */
	public static MutableText warn(String key, MutableText... args) {
		return translatableWithStyle(key, FORMAT_WARN, args);
	}

	/**
	 * Transforms a namespace into a loaded mod's title where possible.
	 * @param namespace a namespace used by the mod.
	 * @return The mod's title if a match or dash-replaced match is found, otherwise namespace.
	 */
	public static String guessModTitle(String namespace) {
		Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(namespace).or(() -> FabricLoader.getInstance().getModContainer(namespace.replace('_', '-')));
		if (mod.isPresent()) {
			return mod.get().getMetadata().getName();
		}
		return namespace;
	}
}

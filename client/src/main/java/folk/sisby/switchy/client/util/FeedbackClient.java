package folk.sisby.switchy.client.util;

import folk.sisby.switchy.util.Feedback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static folk.sisby.switchy.util.Feedback.*;

/**
 * @author Sisby folk
 * @since 2.0.0
 * Utilities for styling text and sending translatable feedback on the client
 */
public class FeedbackClient {

	/**
	 * @param player the player to send the message to
	 * @param text the text to send
	 * Sends the client a Switchy
	 */
	public static void sendClientMessage(ClientPlayerEntity player, Text text) {
		player.sendMessage(literal("[Switchy Client] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	/**
	 * @param player the player to send the message to
	 * @param keyFail a translation key explaining why the user action failed
	 * @param keyCommand a translation key with a suggested command
	 * @param commandArgs the literal arguments to pass to the command text
	 * Sends feedback to the player with a suggested command, using the {@link Feedback#FORMAT_INVALID} style
	 */
	public static void tellInvalidTry(ClientPlayerEntity player, String keyFail, String keyCommand, MutableText... commandArgs) {
		sendClientMessage(player, translatableWithArgs(keyFail, FORMAT_INVALID, translatableWithArgs(keyCommand, commandArgs)));
	}

	/**
	 * @param player the player to send the message to
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * Sends feedback to the player using the {@link Feedback#FORMAT_INVALID} style
	 */
	public static void tellInvalid(ClientPlayerEntity player, String key, MutableText... args) {
		sendClientMessage(player, translatableWithArgs(key, FORMAT_INVALID, args));
	}

	/**
	 * @param player the player to send the message to
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * Sends feedback to the player using the {@link Feedback#FORMAT_SUCCESS} style
	 */
	public static void tellSuccess(ClientPlayerEntity player, String key, MutableText... args) {
		sendClientMessage(player, translatableWithArgs(key, FORMAT_SUCCESS, args));
	}

	/**
	 * @param player the player to send the message to
	 * @param key a translation key
	 * @param args the literal arguments to pass to the translatable text
	 * Sends feedback to the player using the {@link Feedback#FORMAT_WARN} style
	 */
	public static void tellWarn(ClientPlayerEntity player, String key, MutableText... args) {
		sendClientMessage(player, translatableWithArgs(key, FORMAT_WARN, args));
	}
}

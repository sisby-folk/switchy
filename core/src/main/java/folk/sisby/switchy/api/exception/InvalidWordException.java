package folk.sisby.switchy.api.exception;

import com.mojang.brigadier.StringReader;

/**
 * Thrown when the specified string is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class InvalidWordException extends IllegalArgumentException {
	/**
	 * Constructs a {@link InvalidWordException} with the default detail message.
	 */
	public InvalidWordException() {
		super("Specified string argument is not a word.");
	}
}

package folk.sisby.switchy.api.exception;

/**
 * Thrown when a preset e.g. {@link folk.sisby.switchy.api.presets.SwitchyPreset} could not be found by name in the object.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class PresetNotFoundException extends IllegalArgumentException {
	/**
	 * Constructs a {@link PresetNotFoundException} with the default detail message.
	 */
	public PresetNotFoundException() {
		super("Specified preset doesn't exist");
	}
}

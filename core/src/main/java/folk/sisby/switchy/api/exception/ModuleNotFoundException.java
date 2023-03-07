package folk.sisby.switchy.api.exception;

/**
 * Thrown when the identified module e.g. {@link folk.sisby.switchy.api.module.SwitchyModule} was not registered in the object.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class ModuleNotFoundException extends IllegalArgumentException {
	/**
	 * Constructs a {@link ModuleNotFoundException} with the default detail message.
	 */
	public ModuleNotFoundException() {
		super("Specified module doesn't exist.");
	}
}

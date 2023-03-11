package folk.sisby.switchy.api.exception;

/**
 * Thrown to indicate that the specified class was not assignable from the desired object.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class ClassNotAssignableException extends IllegalArgumentException {
	/**
	 * Constructs a {@link ClassNotAssignableException} with a basic detail message.
	 *
	 * @param descriptor a description of the object.
	 * @param obj        the object.
	 * @param clazz      the class.
	 */
	public ClassNotAssignableException(String descriptor, Object obj, Class<?> clazz) {
		super(descriptor + " is defined as " + obj.getClass().getSimpleName() + ", not " + clazz);
	}
}

package dev.sisby.switchy.exception;

public class ComponentFailedInitializeException extends IllegalStateException {
	public ComponentFailedInitializeException() {
	}

	public ComponentFailedInitializeException(String s) {
		super(s);
	}

	public ComponentFailedInitializeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ComponentFailedInitializeException(Throwable cause) {
		super(cause);
	}
}

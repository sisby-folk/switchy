package dev.sisby.switchy.exception;

public class ProfileCurrentException extends IllegalArgumentException {
	public ProfileCurrentException(String profileId) {
		super("%s is the current profile! switch first".formatted(profileId));
	}
}

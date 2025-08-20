package dev.sisby.switchy.exception;

public class ProfileMissingException extends IllegalArgumentException {
	public ProfileMissingException(String profileId) {
		super("profile %s doesn't exist!".formatted(profileId));
	}
}

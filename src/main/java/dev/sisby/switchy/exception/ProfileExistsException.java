package dev.sisby.switchy.exception;

public class ProfileExistsException extends IllegalArgumentException {
	public ProfileExistsException(String profileId) {
		super("profile %s already exists!".formatted(profileId));
	}
}

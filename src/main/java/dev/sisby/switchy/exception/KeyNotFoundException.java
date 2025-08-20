package dev.sisby.switchy.exception;

import net.minecraft.nbt.NbtException;

public class KeyNotFoundException extends NbtException {
	public KeyNotFoundException(String s) {
		super(s);
	}
}

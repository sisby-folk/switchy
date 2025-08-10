package dev.sisby.switchy.util;

import org.apache.commons.lang3.text.WordUtils;

public class FormatUtils {
	@SuppressWarnings("deprecation")
	public static String prettify(String s) {
		return WordUtils.capitalize(s.replace("_", " "));
	}

	public static String ceilHalf(Float f) {
		return "%d%s".formatted((int) Math.ceil(f) / 2, Math.ceil(f) % 2 > 0 ? ".5" : "");
	}
}

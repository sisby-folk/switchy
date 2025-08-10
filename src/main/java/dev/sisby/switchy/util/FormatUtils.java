package dev.sisby.switchy.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.text.WordUtils;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtils {
	@SuppressWarnings("deprecation")
	public static String prettify(String s) {
		return WordUtils.capitalize(s.replace("_", " "));
	}

	public static Text healthText(float f) {
		return Text.empty()
			.append(Text.literal("❤").formatted(Formatting.DARK_RED))
			.append(Text.literal("x").formatted(Formatting.GRAY))
			.append(NumberFormat.getNumberInstance(Locale.ROOT).format(Math.ceil(f) / 2F));
	}
}

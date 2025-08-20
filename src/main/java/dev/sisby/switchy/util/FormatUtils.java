package dev.sisby.switchy.util;

import net.minecraft.item.ItemStack;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import org.apache.commons.lang3.text.WordUtils;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtils {
	@SuppressWarnings("deprecation")
	public static String prettify(String s) {
		return WordUtils.capitalize(s.replace("_", " "));
	}

	public static Text statText(String glyph, float f) {
		return Text.empty()
			.append(Text.literal(glyph).formatted(Formatting.DARK_RED))
			.append(Text.literal("x").formatted(Formatting.GRAY))
			.append(NumberFormat.getNumberInstance(Locale.ROOT).format(Math.ceil(f) / 2F));
	}

	public static Text inventoryText(DefaultedList<ItemStack> inventory) {
		if (inventory.stream().allMatch(ItemStack::isEmpty)) return Text.literal("(empty)").formatted(Formatting.GRAY);
		return Text.empty()
			.styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.empty().append(Text.literal("Contents:\n").formatted(Formatting.GRAY)).append(Texts.join(inventory.stream().filter(i -> !i.isEmpty()).map(i -> Text.empty().append(Text.literal("- ").formatted(Formatting.GRAY)).append(String.valueOf(i.getCount())).append("x ").append(i.getName())).toList(), Text.of("\n"))))))
			.append(String.valueOf(inventory.stream().mapToInt(ItemStack::getCount).sum()))
			.append(Text.literal(" items (").formatted(Formatting.GRAY))
			.append(String.valueOf(inventory.stream().filter(i -> !i.isEmpty()).count()))
			.append(Text.literal(" stacks)").formatted(Formatting.GRAY));
	}
}

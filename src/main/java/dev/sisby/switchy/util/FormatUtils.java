package dev.sisby.switchy.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import org.apache.commons.lang3.text.WordUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FormatUtils {
	@SuppressWarnings("deprecation")
	public static String prettify(String s) {
		return WordUtils.capitalize(s.replace("_", " "));
	}

	public static Text statText(float f) {
		return Text.empty().append(NumberFormat.getNumberInstance(Locale.ROOT).format(Math.ceil(f) / 2F));
	}

	public static Text inventoryText(DefaultedList<ItemStack> inventory) {
		if (inventory == null || inventory.stream().allMatch(ItemStack::isEmpty)) return Text.literal("(empty)").formatted(Formatting.GRAY);
		return Text.empty()
			.styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.empty().append(Text.literal("Contents:\n").formatted(Formatting.GRAY)).append(Texts.join(inventory.stream().filter(i -> !i.isEmpty()).map(i -> Text.empty().append(Text.literal("- ").formatted(Formatting.GRAY)).append(String.valueOf(i.getCount())).append("x ").append(i.getName())).toList(), Text.of("\n"))))))
			.append(String.valueOf(inventory.stream().filter(i -> !i.isEmpty()).count()))
			.append(Text.literal(" stacks").formatted(Formatting.GRAY));
	}

	public static Text nbtPathResultText(List<NbtElement> results, boolean allowHover) {
		if (results.isEmpty()) return Text.literal("(empty)").formatted(Formatting.GRAY);
		if (results.size() > 1 && allowHover) return Text.literal("x%d".formatted(results.size())).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nbtPathResultText(results, false))));
		if (results.size() == 1 && results.get(0) instanceof NbtList l && allowHover) return Text.literal("x%d".formatted(l.size())).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nbtPathResultText(l, false))));
		if (results.size() == 1 && results.get(0) instanceof NbtCompound c && allowHover) return Text.literal("x%d".formatted(c.getKeys().size())).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nbtPathResultText(List.of(c), false))));
		return Texts.join(results.stream().map(element -> allowHover ? Texts.join(NbtHelper.toPrettyPrintedText(element).withoutStyle(), Text.empty()) : NbtHelper.toPrettyPrintedText(element)).toList(), Text.of("\n"));
	}

	public static NbtList decompose(NbtElement element) throws CommandSyntaxException {
		if (element instanceof NbtList l) {
			NbtList decomposed = new NbtList();
			for (NbtElement nbtElement : l) {
				decomposed.add(decompose(nbtElement));
			}
			return decomposed;
		} else if (element instanceof NbtCompound compound) {
			NbtList decomposed = new NbtList();
			for (String key : compound.getKeys()) {
				NbtCompound value = compound.getCompound(key).copy();
				if (!value.isEmpty()) {
					value.putString("key", key);
					decomposed.add(value);
				}
			}
			return decomposed;
		}
		throw NbtPathArgumentType.INVALID_PATH_NODE_EXCEPTION.create();
	}
}

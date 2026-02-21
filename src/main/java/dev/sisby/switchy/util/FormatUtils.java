package dev.sisby.switchy.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtList;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.text.NumberFormat;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class FormatUtils {
	public static final Identifier CHAT_HEADS = Identifier.of("chatheads", "player");

	@SuppressWarnings("deprecation")
	public static String prettify(String s) {
		return WordUtils.capitalize(s.replace("_", " "));
	}

	public static Text statText(float f) {
		return Text.empty().append(NumberFormat.getNumberInstance(Locale.ROOT).format(Math.ceil(f) / 2F));
	}

	public static Text truncate(Object o) {
		return Objects.toString(o).length() <= 10 ? Text.of(Objects.toString(o)) : Text.literal(Objects.toString(o).substring(0, 10) + "...").styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(Objects.toString(o)))));
	}

	public static Text inventoryText(DefaultedList<ItemStack> inventory) {
		if (inventory == null || inventory.stream().allMatch(ItemStack::isEmpty)) return Text.literal("(empty)").formatted(Formatting.GRAY);
		return Text.empty()
			.styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.empty().append(Text.literal("Contents:\n").formatted(Formatting.GRAY)).append(Texts.join(inventory.stream().filter(i -> !i.isEmpty()).map(i -> Text.empty().append(Text.literal("- ").formatted(Formatting.GRAY)).append(String.valueOf(i.getCount())).append("x ").append(i.getName())).toList(), Text.of("\n"))))))
			.append(String.valueOf(inventory.stream().filter(i -> !i.isEmpty()).count()))
			.append(Text.literal(" stacks").formatted(Formatting.GRAY));
	}

	public static Text nbtPathResultText(List<NbtElement> results, boolean allowHover) {
		if (results.isEmpty() || (results.size() == 1 && isEmpty(results.get(0)))) return Text.literal("x0").formatted(Formatting.GRAY);
		if (results.size() > 1 && allowHover) {
			var shortList = Texts.join(results.stream().map(e -> minimalistPrettyPrint(e, 0)).toList(), Text.literal(", "));
			if (shortList.getString().length() < 30) return shortList;
			return Text.literal("x%d".formatted(results.size())).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nbtPathResultText(results, false))));
		}
		if (results.size() == 1 && results.get(0) instanceof NbtList l && allowHover) return nbtPathResultText(l.stream().toList(), true);
		if (results.size() == 1 && results.get(0) instanceof NbtCompound c && allowHover) return Text.literal("x%d".formatted(c.getKeys().size())).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nbtPathResultText(List.of(c), false))));
		return Texts.join(results.stream().map(element -> minimalistPrettyPrint(element, 0)).toList(), Text.of("\n"));
	}

	public static Text minimalistPrettyPrint(NbtElement element, int indent) {
		if (element instanceof NbtCompound compound) {
			return Text.empty().append(Texts.join(compound.getKeys().stream().filter(k -> !isEmpty(compound.get(k))).map(k -> Text.empty().append(Text.literal(k + ": ").formatted(Formatting.GRAY)).append(minimalistPrettyPrint(compound.get(k), indent + 2))).toList(), Text.of("\n" + StringUtils.repeat(' ', indent))));
		} else if (element instanceof AbstractNbtList<?> list) {
			return Text.empty().append(Texts.join(list.stream().filter(e -> !isEmpty(e)).map(e -> Text.empty().append(Text.literal("- ").formatted(Formatting.GRAY)).append(minimalistPrettyPrint(e, indent + 2))).toList(), Text.of("\n" + StringUtils.repeat(' ', indent))));
		} else if (element instanceof AbstractNbtNumber number) {
			return Text.empty().append(Text.literal(NumberFormat.getNumberInstance(Locale.ROOT).format(number.doubleValue())));
		} else if (element instanceof NbtString string) {
			Identifier id = Identifier.tryParse(string.asString());
			if (id != null) {
				return Text.literal(id.getPath()).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(id.toString()))));
			}
			return Text.literal(string.asString());
		}
		return Text.of(element.toString());
	}

	public static boolean isEmpty(NbtElement element) {
		return (element instanceof NbtCompound c && c.isEmpty())
			|| (element instanceof AbstractNbtList<?> l && l.isEmpty())
			|| (element instanceof NbtString s && (s.asString().isBlank() || s.asString().equals("minecraft:air")));
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

	public static Text skin(MinecraftServer server, NbtCompound compound) {
		Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
		MinecraftTexturesPayload payload = gson.fromJson(new String(Base64.getDecoder().decode(compound.getString("value"))), MinecraftTexturesPayload.class);
		MinecraftProfileTexture skinTexture = payload.getTextures().get(MinecraftProfileTexture.Type.SKIN);
		String skinHash = skinTexture.getHash();
		return Placeholders.getPlaceholders().containsKey(CHAT_HEADS) ? Placeholders.parseText(Text.of("%chatheads:player " + skinHash + "%"), PlaceholderContext.of(server)) : truncate(skinHash);
	}

	public static Text stripInteraction(Text text) {
		MutableText mutable = text.copy();
		List<Text> siblings = mutable.getSiblings().stream().map(FormatUtils::stripInteraction).toList();
		mutable.getSiblings().clear();
		mutable.getSiblings().addAll(siblings);
		return stripInteractionNonRecursively(mutable);
	}

	public static Text stripInteractionNonRecursively(Text text) {
		return text.copy().styled(s -> s.withHoverEvent(null).withClickEvent(null).withInsertion(null));
	}
}

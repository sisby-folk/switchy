package dev.sisby.switchy.util;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.component.ResolvableProfile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

public class FormatUtils {

	@SuppressWarnings("deprecation")
	public static String prettify(String s) {
		return WordUtils.capitalize(s.replace("_", " "));
	}

	public static Component statText(float f) {
		return Component.empty().append(NumberFormat.getNumberInstance(Locale.ROOT).format(Math.ceil(f) / 2F));
	}

	public static Component truncate(Object o) {
		return Objects.toString(o).length() <= 10 ? Component.nullToEmpty(Objects.toString(o)) : Component.literal(Objects.toString(o).substring(0, 10) + "...").withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.nullToEmpty(Objects.toString(o)))));
	}

	public static Component inventoryText(NonNullList<ItemStack> inventory) {
		if (inventory == null || inventory.stream().allMatch(ItemStack::isEmpty)) return Component.literal("(empty)").withStyle(ChatFormatting.GRAY);
		return Component.empty()
			.withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.empty().append(Component.literal("Contents:\n").withStyle(ChatFormatting.GRAY)).append(ComponentUtils.formatList(inventory.stream().filter(i -> !i.isEmpty()).map(i -> Component.empty().append(Component.literal("- ").withStyle(ChatFormatting.GRAY)).append(String.valueOf(i.getCount())).append("x ").append(i.getHoverName())).toList(), Component.nullToEmpty("\n"))))))
			.append(String.valueOf(inventory.stream().filter(i -> !i.isEmpty()).count()))
			.append(Component.literal(" stacks").withStyle(ChatFormatting.GRAY));
	}

	public static Component nbtPathResultText(List<Tag> results, boolean allowHover) {
		if (results.isEmpty() || (results.size() == 1 && isEmpty(results.get(0)))) return Component.literal("x0").withStyle(ChatFormatting.GRAY);
		if (results.size() > 1 && allowHover) {
			var shortList = ComponentUtils.formatList(results.stream().map(e -> minimalistPrettyPrint(e, 0)).toList(), Component.literal(", "));
			if (shortList.getString().length() < 30) return shortList;
			return Component.literal("x%d".formatted(results.size())).withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(nbtPathResultText(results, false))));
		}
		if (results.size() == 1 && results.get(0) instanceof ListTag l && allowHover) return nbtPathResultText(l.stream().toList(), true);
		if (results.size() == 1 && results.get(0) instanceof CompoundTag c && allowHover) return Component.literal("x%d".formatted(c.keySet().size())).withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(nbtPathResultText(List.of(c), false))));
		return ComponentUtils.formatList(results.stream().map(element -> minimalistPrettyPrint(element, 0)).toList(), Component.nullToEmpty("\n"));
	}

	public static Component minimalistPrettyPrint(Tag element, int indent) {
		if (element instanceof CompoundTag compound) {
			return Component.empty().append(ComponentUtils.formatList(compound.keySet().stream().filter(k -> !isEmpty(compound.get(k))).map(k -> Component.empty().append(Component.literal(k + ": ").withStyle(ChatFormatting.GRAY)).append(minimalistPrettyPrint(compound.get(k), indent + 2))).toList(), Component.nullToEmpty("\n" + StringUtils.repeat(' ', indent))));
		} else if (element instanceof CollectionTag list) {
			return Component.empty().append(ComponentUtils.formatList(list.stream().filter(e -> !isEmpty(e)).map(e -> Component.empty().append(Component.literal("- ").withStyle(ChatFormatting.GRAY)).append(minimalistPrettyPrint(e, indent + 2))).toList(), Component.nullToEmpty("\n" + StringUtils.repeat(' ', indent))));
		} else if (element instanceof NumericTag number) {
			return Component.empty().append(Component.literal(NumberFormat.getNumberInstance(Locale.ROOT).format(number.asDouble())));
		} else if (element instanceof StringTag string) {
			Identifier id = Identifier.tryParse(string.asString().orElse(""));
			if (id != null) {
				return Component.literal(id.getPath()).withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal(id.toString()))));
			}
			return Component.literal(string.asString().orElse(""));
		}
		return Component.nullToEmpty(element.toString());
	}

	public static Component tag(List<SwitchyComponentTypes.Tag> pairs, String between) {
		return ComponentUtils.formatList(pairs.stream().map(p -> Component.empty().append(Component.literal(p.prefix())).append(Component.literal(between).withStyle(ChatFormatting.GRAY)).append(Component.literal(p.suffix()))).toList(), Component.literal(", ").withStyle(ChatFormatting.GRAY));
	}

	public static boolean isEmpty(Tag element) {
		return (element instanceof CompoundTag c && c.isEmpty())
			|| (element instanceof CollectionTag l && l.isEmpty())
			|| (element instanceof StringTag s && (s.asString().orElse("").isBlank() || s.asString().orElse("").equals("minecraft:air")));
	}

	public static ListTag decompose(Tag element) throws CommandSyntaxException {
		if (element instanceof ListTag l) {
			ListTag decomposed = new ListTag();
			for (Tag nbtElement : l) {
				decomposed.add(decompose(nbtElement));
			}
			return decomposed;
		} else if (element instanceof CompoundTag compound) {
			ListTag decomposed = new ListTag();
			for (String key : compound.keySet()) {
				CompoundTag value = compound.getCompoundOrEmpty(key).copy();
				if (!value.isEmpty()) {
					value.putString("key", key);
					decomposed.add(value);
				}
			}
			return decomposed;
		}
		throw NbtPathArgument.ERROR_INVALID_NODE.create();
	}

	public static Component skin(MinecraftServer server, CompoundTag compound) {
		HashMultimap<String, Property> map = HashMultimap.create();
		map.put("textures", new Property("textures", compound.getString("value").orElse(""), null));
		ResolvableProfile profile = ResolvableProfile.create(Either.right(new ResolvableProfile.Partial(Optional.empty(), Optional.empty(), new PropertyMap(map))), PlayerSkin.Patch.EMPTY);
		return Component.object(new PlayerSprite(profile, true), Component.literal("[?]"));
	}

	public static Component stripInteraction(Component text) {
		MutableComponent mutable = text.copy();
		List<Component> siblings = mutable.getSiblings().stream().map(FormatUtils::stripInteraction).toList();
		mutable.getSiblings().clear();
		mutable.getSiblings().addAll(siblings);
		return stripInteractionNonRecursively(mutable);
	}

	public static Component stripInteractionNonRecursively(Component text) {
		return text.copy().withStyle(s -> s.withHoverEvent(null).withClickEvent(null).withInsertion(null));
	}

	public static Component highlightNameFormat(String nameFormat) {
		Matcher matcher = SwitchyPlayerData.NAME_FORMAT_PATTERN.matcher(nameFormat);
		int printed = 0;
		MutableComponent component = Component.empty();
		while (matcher.find()) {
			component.append(Component.literal(nameFormat.substring(printed, matcher.start())).withStyle(ChatFormatting.GRAY));
			component.append(Component.literal("{").withStyle(ChatFormatting.DARK_GRAY));
			component.append(Component.literal(matcher.group(1)).withStyle(ChatFormatting.GRAY));
			component.append(Component.literal("{").withStyle(ChatFormatting.DARK_GRAY));
			component.append(Component.literal(matcher.group(2)).withStyle(ChatFormatting.WHITE));
			component.append(Component.literal("}").withStyle(ChatFormatting.DARK_GRAY));
			component.append(Component.literal(matcher.group(3)).withStyle(ChatFormatting.GRAY));
			component.append(Component.literal("}").withStyle(ChatFormatting.DARK_GRAY));
			printed = matcher.end();
		}
		component.append(Component.literal(nameFormat.substring(printed)));
		return component;
	}
}

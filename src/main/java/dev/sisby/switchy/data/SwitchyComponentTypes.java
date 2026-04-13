package dev.sisby.switchy.data;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.compat.StyledNicknamesCompat;
import dev.sisby.switchy.util.DispatchMapCodec;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.SwitchyCodecs;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static SwitchyComponentTypes INSTANCE = null;
	private static final SwitchyComponentTypes STATIC = new SwitchyComponentTypes();
	public final Codec<Set<SwitchyComponentType<?>>> SET_CODEC = Codec.list(codec()).xmap(LinkedHashSet::new, ArrayList::new);
	public final Codec<Map<SwitchyComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = DispatchMapCodec.of(codec(), t -> (Codec<Object>) t.codec());

	public static final Map<Identifier, Codec<?>> CODECS = new HashMap<>(Map.ofEntries(
		Map.entry(Identifier.tryBuild("minecraft", "nbt"), SwitchyCodecs.NBT),
		Map.entry(Identifier.tryBuild("minecraft", "byte"), Codec.BYTE),
		Map.entry(Identifier.tryBuild("minecraft", "short"), Codec.SHORT),
		Map.entry(Identifier.tryBuild("minecraft", "int"), Codec.INT),
		Map.entry(Identifier.tryBuild("minecraft", "long"), Codec.LONG),
		Map.entry(Identifier.tryBuild("minecraft", "float"), Codec.FLOAT),
		Map.entry(Identifier.tryBuild("minecraft", "double"), Codec.DOUBLE),
		Map.entry(Identifier.tryBuild("minecraft", "bytes"), Codec.BYTE_BUFFER.xmap(ByteBuffer::array, ByteBuffer::wrap).xmap(Bytes::asList, Bytes::toArray)),
		Map.entry(Identifier.tryBuild("minecraft", "ints"), Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of).xmap(Ints::asList, Ints::toArray)),
		Map.entry(Identifier.tryBuild("minecraft", "longs"), Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of).xmap(Longs::asList, Longs::toArray)),
		Map.entry(Identifier.tryBuild("minecraft", "boolean"), Codec.BOOL),
		Map.entry(Identifier.tryBuild("minecraft", "string"), Codec.STRING),
		Map.entry(Identifier.tryBuild("minecraft", "text"), ExtraCodecs.COMPONENT),
		Map.entry(Identifier.tryBuild("minecraft", "vec3d"), Vec3.CODEC),
		Map.entry(Identifier.tryBuild("minecraft", "identifier"), Identifier.CODEC),
		Map.entry(Identifier.tryBuild("minecraft", "inventory"), SwitchyCodecs.INVENTORY_CODEC)
	));
	public static final Map<Identifier, SwitchyComponentType.TextProvider<?>> TEXT_PROVIDERS = new HashMap<>(Map.of(
		Identifier.tryBuild("minecraft", "trunc"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::truncate),
		Identifier.tryBuild("minecraft", "nbt"), new SwitchyComponentType.SimpleTextProvider<net.minecraft.nbt.Tag>(e -> FormatUtils.nbtPathResultText(List.of(e), true)),
		Identifier.tryBuild("minecraft", "text"), new SwitchyComponentType.SimpleTextProvider<Component>(t -> t),
		Identifier.tryBuild("minecraft", "percent"), new SwitchyComponentType.SimpleTextProvider<Number>(n -> Component.nullToEmpty("%.0f%%".formatted(n.floatValue() * 100.0))),
		Identifier.tryBuild("minecraft", "rounded"), new SwitchyComponentType.SimpleTextProvider<Number>(n -> Component.nullToEmpty("%.0f".formatted(n.floatValue()))),
		Identifier.tryBuild("minecraft", "halves"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::statText),
		Identifier.tryBuild("minecraft", "vec3d"), new SwitchyComponentType.SimpleTextProvider<Vec3>(c -> ComponentUtils.formatList(List.of(
			Component.empty().append(Component.literal("X:").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(BlockPos.containing(c).getX()))),
			Component.empty().append(Component.literal("Y:").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(BlockPos.containing(c).getY()))),
			Component.empty().append(Component.literal("Z:").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(BlockPos.containing(c).getZ())))
		), Component.literal(", ").withStyle(ChatFormatting.GRAY))),
		Identifier.tryBuild("minecraft", "identifier"), new SwitchyComponentType.SimpleTextProvider<Identifier>(i -> Component.nullToEmpty(FormatUtils.prettify(i.getPath()))),
		Identifier.tryBuild("minecraft", "inventory"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::inventoryText),
		Identifier.tryBuild("minecraft", "skin"), new SwitchyComponentType.SimpleServerTextProvider<>(FormatUtils::skin)
	));

	public static final Map<Identifier, SwitchyComponentType.ArgumentEditor<?>> ARGUMENT_EDITORS = new HashMap<>(Map.of(
		Identifier.tryBuild("minecraft", "text"), new SwitchyComponentType.SimpleArgumentEditor<Component>(e -> Commands.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Component.nullToEmpty(c.getArgument("name", String.class)))))
	));
	public static final Map<Identifier, SwitchyComponentType.EmptyChecker<?>> EMPTY_CHECKERS = new HashMap<>(Map.of(
		Identifier.tryBuild("minecraft", "inventory"), new SwitchyComponentType.SimpleEmptyChecker<NonNullList<ItemStack>>(dl -> dl.stream().allMatch(ItemStack::isEmpty))
	));
	public static final Map<Identifier, SwitchyComponentType.Initializer<?>> INITIALIZERS = new HashMap<>(Map.of(
		Identifier.tryBuild("minecraft", "spawn_pos"), (nbt, player, id) -> player.getServer().overworld().getSharedSpawnPos().getCenter()
	));

	public static final Identifier NAME_ID = Switchy.id("name");
	public static final Identifier TAG_ID = Switchy.id("tag");
	public static final Identifier DIMENSION = Identifier.tryBuild("minecraft", "location/dimension");
	public static final Identifier POS = Identifier.tryBuild("minecraft", "location/pos");
	public static final Identifier YAW = Identifier.tryBuild("minecraft", "location/yaw");
	public static final Identifier PITCH = Identifier.tryBuild("minecraft", "location/pitch");
	public static final Identifier SPAWN_X = Identifier.tryBuild("minecraft", "spawn/x");
	public static final Identifier SPAWN_Y = Identifier.tryBuild("minecraft", "spawn/y");
	public static final Identifier SPAWN_Z = Identifier.tryBuild("minecraft", "spawn/z");
	public static final Identifier SPAWN_FORCED = Identifier.tryBuild("minecraft", "spawn/forced");
	public static final Identifier SPAWN_ANGLE = Identifier.tryBuild("minecraft", "spawn/angle");
	public static final Identifier SPAWN_DIMENSION = Identifier.tryBuild("minecraft", "spawn/dimension");
	public static final Identifier EFFECTS = Identifier.tryBuild("minecraft", "effects");
	public static final Identifier HEALTH = Identifier.tryBuild("minecraft", "health");
	public static final Identifier FOOD = Identifier.tryBuild("minecraft", "hunger/food");
	public static final Identifier SATURATION = Identifier.tryBuild("minecraft", "hunger/saturation");
	public static final Identifier EXHAUSTION = Identifier.tryBuild("minecraft", "hunger/exhaustion");
	public static final Identifier XP = Identifier.tryBuild("minecraft", "xp/progress");
	public static final Identifier LEVEL = Identifier.tryBuild("minecraft", "xp/level");
	public static final Identifier INVENTORY = Identifier.tryBuild("minecraft", "inventory/inventory");
	public static final Identifier ENDER_CHEST = Identifier.tryBuild("minecraft", "inventory/ender_chest");
	public static final Identifier ORIGINS_ORIGIN = Identifier.tryBuild("origins", "origin");
	public static final Identifier ORIGINS_POWERS = Identifier.tryBuild("origins", "powers");
	public static final Identifier TAILOR_SKIN = Identifier.tryBuild("fabrictailor", "skin");
	public static final Identifier TRINKETS_SLOTS = Identifier.tryBuild("trinkets", "slots");
	public static final Identifier LAMPBLACK_PRONOUNS = Identifier.tryBuild("lampblack", "pronouns");

	public static final SwitchyComponentType<String> NAME = registerStatic(NAME_ID, Codec.STRING, builder -> {
		builder = builder
			.importable(true)
			.textProvider((server, s) -> Component.literal(s.replaceAll("<?((?:\\\\>|.)*?)>", "")).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.nullToEmpty(s)))))
			.argumentEditor(e -> Commands.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, c.getArgument("name", String.class))));
		return Switchy.STYLED_NICKNAMES ? StyledNicknamesCompat.nameComponent(builder) : builder;
	});

	public record Tag(String prefix, String suffix) {
		public static final Codec<Tag> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.STRING.fieldOf("prefix").forGetter(Tag::prefix),
				Codec.STRING.fieldOf("suffix").forGetter(Tag::suffix)
			).apply(instance, Tag::new)
		);
	}

	public static final SwitchyComponentType<List<Tag>> TAG = registerStatic(TAG_ID, Codec.list(Tag.CODEC), builder -> builder
			.textProvider((server, s) -> FormatUtils.tag(s, "text"))
			.argumentEditor(e -> Commands.argument("prefix_text_suffix", StringArgumentType.greedyString()).executes(c -> {
				String[] rawInput = c.getArgument("prefix_text_suffix", String.class).trim().split("text");
				return e.execute(c, List.of(new Tag(rawInput[0], rawInput.length > 1 ? rawInput[1] : "")));
			}))
	);

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Map<Identifier, List<SwitchyComponentType<?>>> grouped(Set<SwitchyComponentType<?>> keyset) {
		Comparator<Identifier> prioritizedIdComparator = Comparator.comparing((Function<Identifier, Boolean>) id -> !id.getNamespace().equals("switchy"))
			.thenComparing(id -> !id.getNamespace().equals("minecraft"))
			.thenComparing(id -> id);
		Map<Identifier, List<SwitchyComponentType<?>>> grouped = new TreeMap<>(prioritizedIdComparator);
		Comparator<SwitchyComponentType<?>> comparator = Comparator.comparing((Function<SwitchyComponentType<?>, Integer>) SwitchyComponentType::previewPriority, Comparator.reverseOrder())
			.thenComparing(t -> Optional.ofNullable(t.group()).orElse(t.id()), prioritizedIdComparator);
		for (SwitchyComponentType<?> t : keyset.stream().sorted(comparator).toList()) {
			Identifier group = t.group();
			grouped.computeIfAbsent(group != null ? group : t.id(), k -> new ArrayList<>()).add(t);
		}
		return grouped;
	}

	public static <T> SwitchyComponentType<T> registerStatic(Identifier id, Codec<T> codec, UnaryOperator<SwitchyComponentType.Builder<T>> operations) {
		return STATIC.register(id, i -> operations.apply(SwitchyComponentType.builder(i, codec)).build());
	}

	public static void setInstance(SwitchyComponentTypes types) {
		INSTANCE = types;
	}

	public <T> SwitchyComponentType<T> register(Identifier id, Codec<T> codec, UnaryOperator<SwitchyComponentType.Builder<T>> operations) {
		return register(id, i -> operations.apply(SwitchyComponentType.builder(i, codec)).build());
	}

	@Nullable
	public static SwitchyComponentTypes instance() {
		return INSTANCE;
	}

	public static SwitchyComponentTypes getStatic() {
		return STATIC;
	}
}

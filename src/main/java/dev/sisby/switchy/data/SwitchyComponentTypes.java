package dev.sisby.switchy.data;

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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static SwitchyComponentTypes INSTANCE = null;
	private static final SwitchyComponentTypes STATIC = new SwitchyComponentTypes();
	public final Codec<Set<SwitchyComponentType<?>>> SET_CODEC = Codec.list(codec()).xmap(LinkedHashSet::new, ArrayList::new);
	public final Codec<Map<SwitchyComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = DispatchMapCodec.of(codec(), t -> (Codec<Object>) t.codec());

	public static final Map<Identifier, Codec<?>> CODECS = new HashMap<>(Map.of(
		Identifier.of("minecraft", "nbt"), SwitchyCodecs.NBT,
		Identifier.of("minecraft", "boolean"), Codec.BOOL,
		Identifier.of("minecraft", "string"), Codec.STRING,
		Identifier.of("minecraft", "text"), TextCodecs.CODEC,
		Identifier.of("minecraft", "float"), Codec.FLOAT,
		Identifier.of("minecraft", "double"), Codec.DOUBLE,
		Identifier.of("minecraft", "int"), Codec.INT,
		Identifier.of("minecraft", "vec3d"), Vec3d.CODEC,
		Identifier.of("minecraft", "identifier"), Identifier.CODEC,
		Identifier.of("minecraft", "inventory"), SwitchyCodecs.INVENTORY_CODEC
	));
	public static final Map<Identifier, SwitchyComponentType.TextProvider<?>> TEXT_PROVIDERS = new HashMap<>(Map.of(
		Identifier.of("minecraft", "trunc"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::truncate),
		Identifier.of("minecraft", "nbt"), new SwitchyComponentType.SimpleTextProvider<NbtElement>(e -> FormatUtils.nbtPathResultText(List.of(e), true)),
		Identifier.of("minecraft", "text"), new SwitchyComponentType.SimpleTextProvider<Text>(t -> t),
		Identifier.of("minecraft", "percent"), new SwitchyComponentType.SimpleTextProvider<Number>(n -> Text.of("%.0f%%".formatted(n.floatValue() * 100.0))),
		Identifier.of("minecraft", "rounded"), new SwitchyComponentType.SimpleTextProvider<Number>(n -> Text.of("%.0f".formatted(n.floatValue()))),
		Identifier.of("minecraft", "halves"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::statText),
		Identifier.of("minecraft", "vec3d"), new SwitchyComponentType.SimpleTextProvider<Vec3d>(c -> Texts.join(List.of(
			Text.empty().append(Text.literal("X:").formatted(Formatting.GRAY)).append(Text.literal(String.valueOf(BlockPos.ofFloored(c).getX()))),
			Text.empty().append(Text.literal("Y:").formatted(Formatting.GRAY)).append(Text.literal(String.valueOf(BlockPos.ofFloored(c).getY()))),
			Text.empty().append(Text.literal("Z:").formatted(Formatting.GRAY)).append(Text.literal(String.valueOf(BlockPos.ofFloored(c).getZ())))
		), Text.literal(", ").formatted(Formatting.GRAY))),
		Identifier.of("minecraft", "identifier"), new SwitchyComponentType.SimpleTextProvider<Identifier>(i -> Text.of(FormatUtils.prettify(i.getPath()))),
		Identifier.of("minecraft", "inventory"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::inventoryText),
		Identifier.of("minecraft", "skin"), new SwitchyComponentType.SimpleServerTextProvider<>(FormatUtils::skin)
	));

	public static final Map<Identifier, SwitchyComponentType.ArgumentEditor<?>> ARGUMENT_EDITORS = new HashMap<>(Map.of(
		Identifier.of("minecraft", "text"), new SwitchyComponentType.SimpleArgumentEditor<Text>(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Text.of(c.getArgument("name", String.class)))))
	));
	public static final Map<Identifier, SwitchyComponentType.EmptyChecker<?>> EMPTY_CHECKERS = new HashMap<>(Map.of(
		Identifier.of("minecraft", "inventory"), new SwitchyComponentType.SimpleEmptyChecker<DefaultedList<ItemStack>>(dl -> dl.stream().allMatch(ItemStack::isEmpty))
	));
	public static final Map<Identifier, SwitchyComponentType.Initializer<?>> INITIALIZERS = new HashMap<>(Map.of(
		Identifier.of("minecraft", "spawn_pos"), (nbt, player, id) -> player.getServer().getOverworld().getSpawnPos().toCenterPos()
	));

	public static final Identifier NAME_ID = Switchy.id("name");
	public static final Identifier TAG_ID = Switchy.id("tag");
	public static final Identifier DIMENSION = Identifier.of("minecraft", "location/dimension");
	public static final Identifier POS = Identifier.of("minecraft", "location/pos");
	public static final Identifier YAW = Identifier.of("minecraft", "location/yaw");
	public static final Identifier PITCH = Identifier.of("minecraft", "location/pitch");
	public static final Identifier SPAWN_X = Identifier.of("minecraft", "spawn/x");
	public static final Identifier SPAWN_Y = Identifier.of("minecraft", "spawn/y");
	public static final Identifier SPAWN_Z = Identifier.of("minecraft", "spawn/z");
	public static final Identifier SPAWN_FORCED = Identifier.of("minecraft", "spawn/forced");
	public static final Identifier SPAWN_ANGLE = Identifier.of("minecraft", "spawn/angle");
	public static final Identifier SPAWN_DIMENSION = Identifier.of("minecraft", "spawn/dimension");
	public static final Identifier EFFECTS = Identifier.of("minecraft", "effects");
	public static final Identifier HEALTH = Identifier.of("minecraft", "health");
	public static final Identifier FOOD = Identifier.of("minecraft", "hunger/food");
	public static final Identifier SATURATION = Identifier.of("minecraft", "hunger/saturation");
	public static final Identifier EXHAUSTION = Identifier.of("minecraft", "hunger/exhaustion");
	public static final Identifier XP = Identifier.of("minecraft", "xp/progress");
	public static final Identifier LEVEL = Identifier.of("minecraft", "xp/level");
	public static final Identifier INVENTORY = Identifier.of("minecraft", "inventory/inventory");
	public static final Identifier ENDER_CHEST = Identifier.of("minecraft", "inventory/ender_chest");
	public static final Identifier ORIGINS_ORIGIN = Identifier.of("origins", "origin");
	public static final Identifier ORIGINS_POWERS = Identifier.of("origins", "powers");
	public static final Identifier TAILOR_SKIN = Identifier.of("fabrictailor", "skin");
	public static final Identifier TRINKETS_SLOTS = Identifier.of("trinkets", "slots");

	public static final SwitchyComponentType<String> NAME = registerStatic(NAME_ID, Codec.STRING, builder -> {
		builder = builder
			.importable(true)
			.textProvider((server, s) -> Text.literal(s.replaceAll("<?((?:\\\\>|.)*?)>", "")).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(s)))))
			.argumentEditor(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, c.getArgument("name", String.class))));
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
			.argumentEditor(e -> CommandManager.argument("prefix_text_suffix", StringArgumentType.greedyString()).executes(c -> {
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
			.thenComparing(t -> Objects.requireNonNullElse(t.group(), t.id()), prioritizedIdComparator);
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

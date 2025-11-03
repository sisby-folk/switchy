package dev.sisby.switchy.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.compat.StyledNicknamesCompat;
import dev.sisby.switchy.util.DispatchMapCodec;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.SwitchyCodecs;
import dev.sisby.switchy.util.TypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static SwitchyComponentTypes INSTANCE = null;
	private static final SwitchyComponentTypes STATIC = new SwitchyComponentTypes();
	public final Codec<Set<SwitchyComponentType<?>>> SET_CODEC = Codec.list(codec()).xmap(LinkedHashSet::new, ArrayList::new);
	public final Codec<Map<SwitchyComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = DispatchMapCodec.of(codec(), t -> (Codec<Object>) t.codec());

	public static final Map<Identifier, Codec<?>> CODECS = new HashMap<>(Map.of(
		new Identifier("nbt"), SwitchyCodecs.NBT,
		new Identifier("boolean"), Codec.BOOL,
		new Identifier("string"), Codec.STRING,
		new Identifier("text"), Codecs.TEXT,
		new Identifier("float"), Codec.FLOAT,
		new Identifier("int"), Codec.INT,
		new Identifier("vec3d"), Vec3d.CODEC,
		new Identifier("identifier"), Identifier.CODEC,
		new Identifier("inventory"), SwitchyCodecs.INVENTORY_CODEC
	));
	public static final Map<Identifier, SwitchyComponentType.TextProvider<?>> TEXT_PROVIDERS = new HashMap<>(Map.of(
		new Identifier("trunc"), new SwitchyComponentType.SimpleTextProvider<>(o -> Objects.toString(o).length() <= 10 ? Text.of(Objects.toString(o)) : Text.literal(Objects.toString(o).substring(0, 10) + "...").styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(Objects.toString(o)))))),
		new Identifier("nbt"), new SwitchyComponentType.SimpleTextProvider<NbtElement>(e -> Text.literal("%d elements...".formatted(e instanceof NbtCompound c ? c.getSize() : e instanceof NbtList l ? l.size() : 1)).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(Objects.toString(e)))))),
		new Identifier("text"), new SwitchyComponentType.SimpleTextProvider<Text>(t -> t),
		new Identifier("percent"), new SwitchyComponentType.SimpleTextProvider<Float>(n -> Text.of("%.0f%%".formatted(n * 100.0))),
		new Identifier("rounded"), new SwitchyComponentType.SimpleTextProvider<Float>(n -> Text.of("%.0f".formatted(n))),
		new Identifier("halves"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::statText),
		new Identifier("vec3d"), new SwitchyComponentType.SimpleTextProvider<Vec3d>(c -> Text.of(BlockPos.ofFloored(c).toShortString())),
		new Identifier("identifier"), new SwitchyComponentType.SimpleTextProvider<Identifier>(i -> Text.of(FormatUtils.prettify(i.getPath()))),
		new Identifier("inventory"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::inventoryText)
	));
	public static final Map<Identifier, SwitchyComponentType.ArgumentEditor<?>> ARGUMENT_EDITORS = new HashMap<>(Map.of(
		new Identifier("text"), new SwitchyComponentType.SimpleArgumentEditor<Text>(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Text.of(c.getArgument("name", String.class)))))
	));
	public static final Map<Identifier, SwitchyComponentType.EmptyChecker<?>> EMPTY_CHECKERS = new HashMap<>(Map.of(
		new Identifier("inventory"), new SwitchyComponentType.SimpleEmptyChecker<DefaultedList<ItemStack>>(dl -> dl.stream().allMatch(ItemStack::isEmpty))
	));
	public static final Map<Identifier, SwitchyComponentType.Initializer<?>> INITIALIZERS = new HashMap<>(Map.of(
		new Identifier("spawn_pos"), (nbt, player, id) -> player.getServer().getOverworld().getSpawnPos().toCenterPos()
	));
	private static final Identifier ORIGINS_POWERS = new Identifier("origins", "powers");

	public static final Identifier NAME_ID = Switchy.id("name");
	public static final Identifier DIMENSION = new Identifier("minecraft", "location/dimension");
	public static final Identifier FOOD = new Identifier("minecraft", "hunger/food");
	public static final Identifier SATURATION = new Identifier("minecraft", "hunger/saturation");
	public static final Identifier EXHAUSTION = new Identifier("minecraft", "hunger/exhaustion");
	public static final Identifier HEALTH = new Identifier("minecraft", "health");
	public static final Identifier XP = new Identifier("minecraft", "xp/progress");
	public static final Identifier LEVEL = new Identifier("minecraft", "xp/level");
	public static final Identifier POS = new Identifier("minecraft", "location/pos");
	public static final Identifier SPAWN_X = new Identifier("minecraft", "spawn/x");
	public static final Identifier SPAWN_Y = new Identifier("minecraft", "spawn/y");
	public static final Identifier SPAWN_Z = new Identifier("minecraft", "spawn/z");
	public static final Identifier SPAWN_FORCED = new Identifier("minecraft", "spawn/forced");
	public static final Identifier SPAWN_ANGLE = new Identifier("minecraft", "spawn/angle");
	public static final Identifier SPAWN_DIMENSION = new Identifier("minecraft", "spawn/dimension");
	public static final Identifier INVENTORY = new Identifier("minecraft", "inventory/inventory");
	public static final Identifier ENDER_CHEST = new Identifier("minecraft", "inventory/ender_chest");
	public static final Identifier ORIGINS_ORIGIN = new Identifier("origins", "origin");
	public static final Identifier TAILOR_SKIN = new Identifier("fabrictailor", "skin");
	public static final Identifier TRINKETS_SLOTS = new Identifier("trinkets", "slots");

	public static final SwitchyComponentType<String> NAME = registerStatic(NAME_ID, Codec.STRING, builder -> {
		builder = builder
			.textProvider(s -> s != null ? Text.literal(s) : Text.empty())
			.argumentEditor(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, c.getArgument("name", String.class))));
		return FabricLoader.getInstance().isModLoaded("styled-nicknames") ? StyledNicknamesCompat.nicknameComponent(builder) : builder;
	});

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Map<Identifier, List<SwitchyComponentType<?>>> grouped(Set<SwitchyComponentType<?>> keyset) {
		Map<Identifier, List<SwitchyComponentType<?>>> grouped = new LinkedHashMap<>();
		for (SwitchyComponentType<?> t : keyset.stream().sorted(Comparator.comparing(t -> t.id().toString())).toList()) {
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

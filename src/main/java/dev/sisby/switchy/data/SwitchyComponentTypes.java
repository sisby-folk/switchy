package dev.sisby.switchy.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.compat.StyledNicknamesCompat;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

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
	private static final Identifier ORIGINS_POWERS = new Identifier("origins", "powers");

	public record EditableComponentType(boolean enabled, String codec, String path, String preview, String prefix, String editor, String emptyChecker, String group) {
	}

	private static final java.lang.reflect.Type EDITABLE_COMPONENT_TYPE = new TypeToken<EditableComponentType>() {
	}.getType();

	public static final SwitchyComponentType<String> NAME = register(Switchy.id("name"), Codec.STRING, builder -> {
		builder = builder
			.textProvider(s -> s != null ? Text.literal(s) : Text.empty())
			.argumentEditor(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, c.getArgument("name", String.class))));
		return FabricLoader.getInstance().isModLoaded("styled-nicknames") ? StyledNicknamesCompat.nicknameComponent(builder) : builder;
	});

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
	public static final Map<Identifier, EditableComponentType> DEFAULT_COMPONENTS = Map.ofEntries(
		// minecraft
		Map.entry(DIMENSION, new EditableComponentType(true, "identifier", "Dimension", "identifier", null, null, null, "location")),
		Map.entry(FOOD, new EditableComponentType(true, "float", "foodLevel", "halves", "🍖x", null, null, "hunger")),
		Map.entry(SATURATION, new EditableComponentType(true, "float", "foodSaturationLevel", "halves", "+🍖x", null, null, "hunger")),
		Map.entry(EXHAUSTION, new EditableComponentType(true, "float", "foodExhaustionLevel", "halves", "-💨x", null, null, "hunger")),
		Map.entry(HEALTH, new EditableComponentType(true, "float", "Health", "halves", "❤x", null, null, null)),
		Map.entry(XP, new EditableComponentType(true, "float", "XpP", "percent", null, null, null, "xp")),
		Map.entry(LEVEL, new EditableComponentType(true, "int", "XpLevel", null, "Lv.", null, null, "xp")),
		Map.entry(POS, new EditableComponentType(true, "vec3d", "Pos", "vec3d", null, null, null, "location")),
		Map.entry(SPAWN_X, new EditableComponentType(true, "int", "SpawnX", null, "X:", null, null, "spawn")),
		Map.entry(SPAWN_Y, new EditableComponentType(true, "int", "SpawnY", null, "Y:", null, null, "spawn")),
		Map.entry(SPAWN_Z, new EditableComponentType(true, "int", "SpawnZ", null, "Z:", null, null, "spawn")),
		Map.entry(SPAWN_FORCED, new EditableComponentType(true, "int", "SpawnForced", null, "🛏:", null, null, "spawn")),
		Map.entry(SPAWN_ANGLE, new EditableComponentType(true, "float", "SpawnAngle", "rounded", "°", null, null, "spawn")),
		Map.entry(SPAWN_DIMENSION, new EditableComponentType(true, "identifier", "SpawnDimension", "identifier", null, null, null, "spawn")),
		Map.entry(INVENTORY, new EditableComponentType(true, "inventory", "Inventory", "inventory", "🧰 ", null, "inventory", "inventory")),
		Map.entry(ENDER_CHEST, new EditableComponentType(true, "inventory", "EnderItems", "inventory", "👁 ", null, "inventory", "inventory")),
		// origins
		Map.entry(ORIGINS_ORIGIN, new EditableComponentType(true, "identifier", "cardinal_components.origins:origin.OriginLayers", "identifier", null, null, null, "origins:origin")),
		Map.entry(ORIGINS_POWERS, new EditableComponentType(true, "nbt", "cardinal_components.apoli:powers.Powers", "nbt", null, null, null, "origins:origin")),
		// fabric tailor
		Map.entry(TAILOR_SKIN, new EditableComponentType(true, "nbt", "fabrictailor:skin_data", "nbt", null, null, null, null)),
		// trinkets
		Map.entry(TRINKETS_SLOTS, new EditableComponentType(true, "nbt", "cardinal_components.trinkets:trinkets", "nbt", "💍 ", null, null, "inventory"))
	);

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Map<Identifier, List<SwitchyComponentType<?>>> grouped(Set<SwitchyComponentType<?>> keyset) {
		Map<Identifier, List<SwitchyComponentType<?>>> grouped = new LinkedHashMap<>();
		for (SwitchyComponentType<?> t : keyset.stream().sorted(Comparator.comparing(t -> t.id().toString())).toList()) {
			Identifier group = t.group();
			grouped.computeIfAbsent(group != null ? group : t.id(), k -> new ArrayList<>()).add(t);
		}
		return grouped;
	}

	public static void init() {
		File componentsFolder = FabricLoader.getInstance().getConfigDir().resolve(Switchy.ID).resolve("components").toFile();
		try {
			// Create missing defaults
			for (Map.Entry<Identifier, EditableComponentType> entry : DEFAULT_COMPONENTS.entrySet()) {
				Identifier key = entry.getKey();
				EditableComponentType config = entry.getValue();
				if (!FabricLoader.getInstance().isModLoaded(key.getNamespace())) continue;
				File componentFile = componentsFolder.toPath().resolve(key.toString().replace(":", "/") + ".json").toFile();
				if (!componentFile.exists()) {
					componentFile.getParentFile().mkdirs();
					try (Writer writer = new FileWriter(componentFile)) {
						GSON.toJson(config, writer);
					}
				}
			}
			try (Stream<Path> paths = Files.walk(componentsFolder.toPath())) {
				for (Path path : paths.toList()) {
					for (String fileName : Objects.requireNonNullElse(path.toFile().list((dir, name) -> name.endsWith(".json")), new String[]{})) {
						File file = path.resolve(fileName).toFile();
						Identifier id = new Identifier(componentsFolder.toPath().relativize(file.toPath()).toString().replace("\\", "/").replace(".json", "").replaceFirst("/", ":"));
						if (!FabricLoader.getInstance().isModLoaded(id.getNamespace())) {
							Switchy.LOGGER.warn("[Switchy] Skipping loading enabled module {} as mod {} is not loaded", id, id.getNamespace());
							continue;
						}
						EditableComponentType config = GSON.fromJson(new JsonReader(new FileReader(file)), EDITABLE_COMPONENT_TYPE);
						if (!config.enabled) continue;
						Codec<?> codec = CODECS.get(config.codec != null && CODECS.containsKey(Identifier.tryParse(config.codec)) ? Identifier.tryParse(config.codec) : Identifier.tryParse("nbt"));
						registerConfig(codec, id, config);
					}
				}
			}
		} catch (IOException e) {
			// do something
			throw new RuntimeException(e);
		}
		Switchy.LOGGER.info("[Switchy] Initialized {} component types: {}", INSTANCE.keys().size(), INSTANCE.keys().stream().sorted().toList());
	}

	public static <T> void registerConfig(Codec<T> codec, Identifier id, EditableComponentType config) {
		SwitchyComponentType.TextProvider<T> provider = config.preview == null ? null :  (SwitchyComponentType.TextProvider<T>) TEXT_PROVIDERS.get(Identifier.tryParse(config.preview));
		SwitchyComponentType.ArgumentEditor<T> editor = config.editor == null ? null : (SwitchyComponentType.ArgumentEditor<T>) ARGUMENT_EDITORS.get(Identifier.tryParse(config.editor));
		SwitchyComponentType.EmptyChecker<T> checker = config.emptyChecker == null ? null :  (SwitchyComponentType.EmptyChecker<T>) EMPTY_CHECKERS.get(Identifier.tryParse(config.emptyChecker));
		register(id, codec, b -> b
			.nbtSwitcher(config.path)
			.textProvider(v -> Text.empty().append(Text.literal(Objects.requireNonNullElse(config.prefix, "")).formatted(Formatting.GRAY)).append(provider != null ? provider.toText(v) : Text.of(Objects.toString(v))))
			.argumentEditor(editor)
			.emptyChecker(checker)
			.group(config.group == null ? null : Identifier.tryParse(config.group))
		);
	}

	public static <T> SwitchyComponentType<T> register(Identifier id, Codec<T> codec, UnaryOperator<SwitchyComponentType.Builder<T>> operations) {
		return instance().register(id, i -> operations.apply(SwitchyComponentType.builder(i, codec)).build());
	}

	public static SwitchyComponentTypes instance() {
		return INSTANCE;
	}
}

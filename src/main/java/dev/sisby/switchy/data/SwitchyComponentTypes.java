package dev.sisby.switchy.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.SwitchyCodecs;
import dev.sisby.switchy.util.TypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

	public static final Map<Identifier, Codec<?>> CODECS = new HashMap<>(Map.of(
		new Identifier("nbt"), SwitchyCodecs.NBT,
		new Identifier("string"), Codec.STRING,
		new Identifier("text"), Codecs.TEXT,
		new Identifier("float"), Codec.FLOAT,
		new Identifier("vec3d"), Vec3d.CODEC,
		new Identifier("identifier"), Identifier.CODEC,
		new Identifier("inventory"), SwitchyCodecs.INVENTORY_CODEC
	));
	public static final Map<Identifier, SwitchyComponentType.TextProvider<?>> TEXT_PROVIDERS = new HashMap<>(Map.of(
		new Identifier("text"), new SwitchyComponentType.SimpleTextProvider<Text>(t -> t),
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

	public record EditableComponentType(boolean enabled, String codec, String path, String preview, String prefix, String editor, String emptyChecker) {
	}

	private static final java.lang.reflect.Type EDITABLE_COMPONENT_TYPE = new TypeToken<EditableComponentType>() {
	}.getType();

	public static final SwitchyComponentType<Text> NAME = register(Switchy.id("name"), Codecs.TEXT, b -> b.textProvider(c -> c)
		.argumentEditor(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Text.of(c.getArgument("name", String.class))))));

	public static final Map<Identifier, EditableComponentType> DEFAULT_COMPONENTS = Map.of(
		new Identifier("minecraft", "dimension"), new EditableComponentType(true, "identifier", "Dimension", "identifier", null, null, null),
		new Identifier("minecraft", "food"), new EditableComponentType(true, "float", "foodLevel", "halves", "🍖x", null, null),
		new Identifier("minecraft", "saturation"), new EditableComponentType(true, "float", "foodSaturationLevel", "halves", "+🍖x", null, null),
		new Identifier("minecraft", "exhaustion"), new EditableComponentType(true, "float", "foodExhaustionLevel", "halves", "-💨x", null, null),
		new Identifier("minecraft", "health"), new EditableComponentType(true, "float", "Health", "halves", "❤x", null, null),
		new Identifier("minecraft", "pos"), new EditableComponentType(true, "vec3d", "Pos", "vec3d", null, null, null),
		new Identifier("minecraft", "inventory"), new EditableComponentType(true, "inventory", "Inventory", "inventory", null, null, "inventory")
	);

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void init() {
		File componentsFolder = FabricLoader.getInstance().getConfigDir().resolve(Switchy.ID).resolve("components").toFile();
		try {
			// Create missing defaults
			for (Map.Entry<Identifier, EditableComponentType> entry : DEFAULT_COMPONENTS.entrySet()) {
				Identifier key = entry.getKey();
				EditableComponentType config = entry.getValue();
				if (!FabricLoader.getInstance().isModLoaded(key.getNamespace())) continue;
				File componentFile = componentsFolder.toPath().resolve(key.toString().replace("minecraft:", "").replace(":", "/") + ".json").toFile();
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
						Identifier id = new Identifier(componentsFolder.toPath().relativize(file.toPath()).toString().replace(".json", "").replaceFirst("/", ":"));
						if (!FabricLoader.getInstance().isModLoaded(id.getNamespace())) return;
						EditableComponentType config = GSON.fromJson(new JsonReader(new FileReader(file)), EDITABLE_COMPONENT_TYPE);
						if (!config.enabled) return;
						Codec<?> codec = CODECS.get(CODECS.containsKey(Identifier.tryParse(config.codec)) ? Identifier.tryParse(config.codec) : Identifier.tryParse("nbt"));
						registerConfig(codec, id, config);
					}
				}
			}
		} catch (IOException e) {
			// do something
			throw new RuntimeException(e);
		}
	}

	public static <T> void registerConfig(Codec<T> codec, Identifier id, EditableComponentType config) {
		SwitchyComponentType.TextProvider<T> provider = config.preview == null ? null :  (SwitchyComponentType.TextProvider<T>) TEXT_PROVIDERS.get(Identifier.tryParse(config.preview));
		SwitchyComponentType.ArgumentEditor<T> editor = config.editor == null ? null : (SwitchyComponentType.ArgumentEditor<T>) ARGUMENT_EDITORS.get(Identifier.tryParse(config.editor));
		SwitchyComponentType.EmptyChecker<T> checker = config.emptyChecker == null ? null :  (SwitchyComponentType.EmptyChecker<T>) EMPTY_CHECKERS.get(Identifier.tryParse(config.emptyChecker));
		register(id, codec, b -> b
			.nbtSwitcher(config.path)
			.textProvider(provider != null && config.prefix != null ? (v -> Text.empty().append(Text.literal(config.prefix).formatted(Formatting.GRAY)).append(provider.toText(v))) : provider)
			.argumentEditor(editor)
			.emptyChecker(checker)
		);
	}

	public static <T> SwitchyComponentType<T> register(Identifier id, Codec<T> codec, UnaryOperator<SwitchyComponentType.Builder<T>> operations) {
		return instance().register(id, i -> operations.apply(SwitchyComponentType.builder(i, codec)).build());
	}

	public static SwitchyComponentTypes instance() {
		return INSTANCE;
	}
}

package dev.sisby.switchy.data;

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
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

	public static final Map<Identifier, Codec<?>> CODECS = new HashMap<>(Map.of(
		Identifier.ofVanilla("string"), Codec.STRING,
		Identifier.ofVanilla("text"), TextCodecs.CODEC,
		Identifier.ofVanilla("float"), Codec.FLOAT,
		Identifier.ofVanilla("vec3d"), Vec3d.CODEC,
		Identifier.ofVanilla("identifier"), Identifier.CODEC,
		Identifier.ofVanilla("inventory"), SwitchyCodecs.INVENTORY_CODEC
	));
	public static final Map<Identifier, SwitchyComponentType.TextProvider<?>> TEXT_PROVIDERS = new HashMap<>(Map.of(
		Identifier.ofVanilla("text"), new SwitchyComponentType.SimpleTextProvider<Text>(t -> t),
		Identifier.ofVanilla("halves"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::statText),
		Identifier.ofVanilla("vec3d"), new SwitchyComponentType.SimpleTextProvider<Vec3d>(c -> Text.of(BlockPos.ofFloored(c).toShortString())),
		Identifier.ofVanilla("identifier"), new SwitchyComponentType.SimpleTextProvider<Identifier>(i -> Text.of(FormatUtils.prettify(i.getPath()))),
		Identifier.ofVanilla("inventory"), new SwitchyComponentType.SimpleTextProvider<>(FormatUtils::inventoryText)
	));
	public static final Map<Identifier, SwitchyComponentType.ArgumentEditor<?>> ARGUMENT_EDITORS = new HashMap<>(Map.of(
		Identifier.ofVanilla("text"), new SwitchyComponentType.SimpleArgumentEditor<Text>(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Text.of(c.getArgument("name", String.class)))))
	));
	public static final Map<Identifier, SwitchyComponentType.EmptyChecker<?>> EMPTY_CHECKERS = new HashMap<>(Map.of(
		Identifier.ofVanilla("inventory"), new SwitchyComponentType.SimpleEmptyChecker<DefaultedList<ItemStack>>(dl -> dl.stream().allMatch(ItemStack::isEmpty))
	));

	public static final SwitchyComponentType<Text> NAME = register(Switchy.id("name"), TextCodecs.CODEC, b -> b.textProvider(c -> c)
		.argumentEditor(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Text.of(c.getArgument("name", String.class))))));

	public static void init() {
		File componentsFolder = FabricLoader.getInstance().getConfigDir().resolve(Switchy.ID).resolve("components").toFile();
		componentsFolder.mkdirs();
		for (String file : Objects.requireNonNullElse(componentsFolder.list((dir, name) -> name.endsWith(".toml")), new String[]{})) {
			SwitchyConfigComponentType config = SwitchyConfigComponentType.createToml(FabricLoader.getInstance().getConfigDir(), "%s/components".formatted(Switchy.ID), file.replace(".toml", ""), SwitchyConfigComponentType.class);
			Codec<?> codec = CODECS.get(Identifier.tryParse(config.codec));
			registerConfig(codec, config);
		}
	}

	public static <T> void registerConfig(Codec<T> codec, SwitchyConfigComponentType config) {
		Identifier id = Identifier.tryParse(config.id());
		SwitchyComponentType.TextProvider<T> provider = (SwitchyComponentType.TextProvider<T>) TEXT_PROVIDERS.get(Identifier.tryParse(config.preview));
		SwitchyComponentType.ArgumentEditor<T> editor = (SwitchyComponentType.ArgumentEditor<T>) ARGUMENT_EDITORS.get(Identifier.tryParse(config.editor));
		SwitchyComponentType.EmptyChecker<T> checker = (SwitchyComponentType.EmptyChecker<T>) EMPTY_CHECKERS.get(Identifier.tryParse(config.emptyChecker));
		register(id, codec, b -> b
			.nbtSwitcher(config.path)
			.textProvider(provider != null && !config.prefix.isEmpty() ? (v -> Text.empty().append(Text.literal(config.prefix).formatted(Formatting.GRAY)).append(provider.toText(v))) : provider)
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

package dev.sisby.switchy.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.SwitchyCodecs;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ComponentTypeLoader extends SimpleJsonResourceReloadListener<ComponentTypeLoader.EditableComponentType> implements IdentifiableResourceReloadListener {
	public static final String PATH = "switchy_components";
	public record EditableComponentType(NbtPathArgument.NbtPath path, boolean enabled, Identifier codec, Optional<String> preview, String prefix, Optional<Identifier> editor, Optional<String> emptyChecker, Optional<Identifier> group, Optional<Tag> defaultValue, boolean hidden, boolean importable, int priority) {
		public static Codec<EditableComponentType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			NbtPathArgument.NbtPath.CODEC.fieldOf("path").forGetter(EditableComponentType::path),
			Codec.BOOL.optionalFieldOf("enabled", true).forGetter(EditableComponentType::enabled),
			Identifier.CODEC.optionalFieldOf("codec", Identifier.withDefaultNamespace("nbt")).forGetter(EditableComponentType::codec),
			Codec.STRING.optionalFieldOf("preview").forGetter(EditableComponentType::preview),
			Codec.STRING.optionalFieldOf("prefix", "").forGetter(EditableComponentType::prefix),
			Identifier.CODEC.optionalFieldOf("editor").forGetter(EditableComponentType::editor),
			Codec.STRING.optionalFieldOf("emptyChecker").forGetter(EditableComponentType::emptyChecker),
			Identifier.CODEC.optionalFieldOf("group").forGetter(EditableComponentType::group),
			SwitchyCodecs.NBT.optionalFieldOf("default").forGetter(EditableComponentType::defaultValue),
			Codec.BOOL.optionalFieldOf("hidden", false).forGetter(EditableComponentType::hidden),
			Codec.BOOL.optionalFieldOf("importable", false).forGetter(EditableComponentType::importable),
			Codec.INT.optionalFieldOf("priority", 0).forGetter(EditableComponentType::priority)
		).apply(instance, EditableComponentType::new));
	}

	public ComponentTypeLoader() {
		super(EditableComponentType.CODEC, FileToIdConverter.json(PATH));
	}

	@Override
	protected void apply(Map<Identifier, EditableComponentType> prepared, ResourceManager manager, ProfilerFiller profiler) {
		SwitchyComponentTypes types = new SwitchyComponentTypes();
		for (Identifier id : SwitchyComponentTypes.getStatic().keys()) { // re-apply static types e.g. NAME
			types.register(id, i -> SwitchyComponentTypes.getStatic().get(id));
		}
		for (Identifier id : prepared.keySet()) {
			EditableComponentType type = prepared.get(id);
			if (!Switchy.CONFIG.isEnabled(id, type.enabled)) continue;
			Codec<?> codec = SwitchyComponentTypes.CODECS.get(SwitchyComponentTypes.CODECS.containsKey(type.codec) ? type.codec : Identifier.tryParse("nbt"));
			registerDataType(types, codec, id, type);
		}
		SwitchyComponentTypes.setInstance(types);
		Switchy.LOGGER.info("[Switchy] Initialized {} component types: {}", types.keys().size(), types.keys().stream().sorted().toList());
	}

	@SuppressWarnings("unchecked")
	public static <T> void registerDataType(SwitchyComponentTypes types, Codec<T> codec, Identifier id, EditableComponentType type) {
		SwitchyComponentType.TextProvider<T> provider = type.preview.map(string -> (SwitchyComponentType.TextProvider<T>) SwitchyComponentTypes.TEXT_PROVIDERS.get(Identifier.tryParse(string))).orElse(null);
		SwitchyComponentType.ArgumentEditor<T> editor = type.editor.map(identifier -> (SwitchyComponentType.ArgumentEditor<T>) SwitchyComponentTypes.ARGUMENT_EDITORS.get(identifier)).orElse(null);
		SwitchyComponentType.EmptyChecker<T> checker = type.emptyChecker.map(string -> (SwitchyComponentType.EmptyChecker<T>) SwitchyComponentTypes.EMPTY_CHECKERS.get(Identifier.tryParse(string))).orElse(null);
		SwitchyComponentType.Initializer<T> initializer;
		if (type.defaultValue.isEmpty()) {
			initializer = (nbt, player, pId) -> null;
		} else if (type.defaultValue.get() instanceof StringTag st && st.asString().orElse("").equals("\"$copy\"")) { // lazy
			initializer = null;
		} else if (type.defaultValue.get() instanceof StringTag st && st.asString().orElse("").startsWith("$")) {
			initializer = (SwitchyComponentType.Initializer<T>) SwitchyComponentTypes.INITIALIZERS.get(Identifier.tryParse(st.asString().orElse(" ").substring(1)));
		} else {
			T defaultValue = codec.parse(NbtOps.INSTANCE, type.defaultValue.get()).getOrThrow();
			initializer = (nbt, player, pId) -> defaultValue;
		}
		try {
			if (provider == null) {
				if (type.preview.isPresent() && type.preview.get().startsWith("$")) {
					String nbtPath = type.preview.get().substring(1);
					int decompositions = 0;
					while (nbtPath.startsWith("*")) {
						nbtPath = nbtPath.substring(1);
						decompositions++;
					}
					NbtPathArgument.NbtPath previewPath = NbtPathArgument.nbtPath().parse(new StringReader(nbtPath));
					int finalDecompositions = decompositions;
					provider = (server, v) -> {
						try {
							Tag element = (Tag) v;
							int decomposed = 0;
							while (decomposed < finalDecompositions) {
								element = FormatUtils.decompose(element);
								decomposed++;
							}
							return FormatUtils.nbtPathResultText(previewPath.get(element), true);
						} catch (CommandSyntaxException e) {
							return FormatUtils.nbtPathResultText(List.of(), true);
						}
					};
				} else {
					provider = (server, v) -> Component.literal(Objects.toString(v));
				}
			}
			if (checker == null) {
				if (type.emptyChecker.isPresent() && type.emptyChecker.get().startsWith("$")) {
					String nbtPath = type.emptyChecker.get().substring(1);
					int decompositions = 0;
					while (nbtPath.startsWith("*")) {
						nbtPath = nbtPath.substring(1);
						decompositions++;
					}
					NbtPathArgument.NbtPath checkerPath = NbtPathArgument.nbtPath().parse(new StringReader(nbtPath));
					int finalDecompositions = decompositions;
					checker = v -> {
						try {
							Tag element = (Tag) v;
							int decomposed = 0;
							while (decomposed < finalDecompositions) {
								element = FormatUtils.decompose(element);
								decomposed++;
							}
							return checkerPath.get(element).stream().allMatch(FormatUtils::isEmpty);
						} catch (CommandSyntaxException e) {
							return true;
						}
					};
				}
			}
			SwitchyComponentType.TextProvider<T> finalProvider = provider;
			SwitchyComponentType.TextProvider<T> prefixedPreviewer = (server, v) -> Component.empty().append(Component.literal(Objects.requireNonNullElse(type.prefix, "")).withStyle(ChatFormatting.GRAY)).append(finalProvider.toText(server, v));
			SwitchyComponentType.EmptyChecker<T> finalChecker = checker;
			types.register(id, codec, b -> b
				.nbtSwitcher(type.path)
				.textProvider(prefixedPreviewer)
				.argumentEditor(editor)
				.emptyChecker(finalChecker)
				.group(type.group.orElse(null))
				.hidden(type.hidden)
				.importable(type.importable)
				.previewPriority(type.priority)
				.initializer(initializer) // overrides switcher
			);
		} catch (CommandSyntaxException e) {
			Switchy.LOGGER.error("[Switchy] Failed to register component type {} due to a malformed NBT path", id);
		}
	}

	@Override
	public Identifier getFabricId() {
		return Switchy.id(PATH);
	}
}

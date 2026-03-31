package dev.sisby.switchy.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.FormatUtils;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ComponentTypeLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
	public static final String PATH = "switchy_components";
	public static final Gson GSON = new Gson();
	public record EditableComponentType(boolean enabled, String codec, String path, String preview, String prefix, String editor, String emptyChecker, String group, @SerializedName("default") JsonElement defaultValue, Boolean hidden, Boolean importable, Integer priority) { }

	public ComponentTypeLoader() {
		super(GSON, PATH);
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
		SwitchyComponentTypes types = new SwitchyComponentTypes();
		for (Identifier id : SwitchyComponentTypes.getStatic().keys()) { // re-apply static types e.g. NAME
			types.register(id, i -> SwitchyComponentTypes.getStatic().get(id));
		}
		for (Identifier id : prepared.keySet()) {
			EditableComponentType type = GSON.fromJson(prepared.get(id), EditableComponentType.class);
			if (!Switchy.CONFIG.isEnabled(id, type.enabled)) continue;
			Codec<?> codec = SwitchyComponentTypes.CODECS.get(type.codec != null && SwitchyComponentTypes.CODECS.containsKey(Identifier.tryParse(type.codec)) ? Identifier.tryParse(type.codec) : Identifier.tryParse("nbt"));
			registerDataType(types, codec, id, type);
		}
		SwitchyComponentTypes.setInstance(types);
		Switchy.LOGGER.info("[Switchy] Initialized {} component types: {}", types.keys().size(), types.keys().stream().sorted().toList());
	}

	public static <T> void registerDataType(SwitchyComponentTypes types, Codec<T> codec, Identifier id, EditableComponentType type) {
		SwitchyComponentType.TextProvider<T> provider = type.preview == null ? null : (SwitchyComponentType.TextProvider<T>) SwitchyComponentTypes.TEXT_PROVIDERS.get(Identifier.tryParse(type.preview));
		SwitchyComponentType.ArgumentEditor<T> editor = type.editor == null ? null : (SwitchyComponentType.ArgumentEditor<T>) SwitchyComponentTypes.ARGUMENT_EDITORS.get(Identifier.tryParse(type.editor));
		SwitchyComponentType.EmptyChecker<T> checker = type.emptyChecker == null ? null : (SwitchyComponentType.EmptyChecker<T>) SwitchyComponentTypes.EMPTY_CHECKERS.get(Identifier.tryParse(type.emptyChecker));
		SwitchyComponentType.Initializer<T> initializer;
		if (type.defaultValue == null) {
			initializer = (nbt, player, pId) -> null;
		} else if (type.defaultValue.toString().equals("\"$copy\"")) { // lazy
			initializer = null;
		} else if (type.defaultValue.isJsonPrimitive() && type.defaultValue.getAsJsonPrimitive().isString() && type.defaultValue.getAsJsonPrimitive().getAsString().startsWith("$")) {
			initializer = (SwitchyComponentType.Initializer<T>) SwitchyComponentTypes.INITIALIZERS.get(Identifier.tryParse(type.defaultValue.getAsString().substring(1)));
		} else {
			T defaultValue = codec.parse(JsonOps.INSTANCE, type.defaultValue).getOrThrow(false, Switchy.LOGGER::error);
			initializer = (nbt, player, pId) -> defaultValue;
		}
		try {
			if (provider == null) {
				if (type.preview != null && type.preview.startsWith("$")) {
					String nbtPath = type.preview.substring(1);
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
				if (type.emptyChecker != null && type.emptyChecker.startsWith("$")) {
					String nbtPath = type.emptyChecker.substring(1);
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
			NbtPathArgument.NbtPath path = NbtPathArgument.nbtPath().parse(new StringReader(type.path));
			SwitchyComponentType.EmptyChecker<T> finalChecker = checker;
			types.register(id, codec, b -> b
				.nbtSwitcher(path)
				.textProvider(prefixedPreviewer)
				.argumentEditor(editor)
				.emptyChecker(finalChecker)
				.group(type.group == null ? null : Identifier.tryParse(type.group))
				.hidden(type.hidden != null && type.hidden)
				.importable(type.importable != null && type.importable)
				.previewPriority(type.priority == null ? 0 : type.priority)
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

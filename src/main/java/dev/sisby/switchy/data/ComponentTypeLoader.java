package dev.sisby.switchy.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.sisby.switchy.Switchy;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Map;
import java.util.Objects;

public class ComponentTypeLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
	public static final String PATH = "switchy_components";
	public static final Gson GSON = new Gson();
	public record EditableComponentType(boolean enabled, String codec, String path, String preview, String prefix, String editor, String emptyChecker, String group, @SerializedName("default") JsonElement defaultValue) { }

	public ComponentTypeLoader() {
		super(GSON, PATH);
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		SwitchyComponentTypes types = new SwitchyComponentTypes();
		for (Identifier id : SwitchyComponentTypes.getStatic().keys()) { // re-apply static types e.g. NAME
			types.register(id, i -> SwitchyComponentTypes.getStatic().get(id));
		}
		for (Identifier id : prepared.keySet()) {
			EditableComponentType type = GSON.fromJson(prepared.get(id), EditableComponentType.class);
			if (!type.enabled()) continue;
			Codec<?> codec = SwitchyComponentTypes.CODECS.get(type.codec != null && SwitchyComponentTypes.CODECS.containsKey(Identifier.tryParse(type.codec)) ? Identifier.tryParse(type.codec) : Identifier.tryParse("nbt"));
			registerDataType(types, codec, id, type);
		}
		SwitchyComponentTypes.setInstance(types);
		Switchy.LOGGER.info("[Switchy] Initialized {} component types: {}", types.keys().size(), types.keys().stream().sorted().toList());
	}

	public static <T> void registerDataType(SwitchyComponentTypes types, Codec<T> codec, Identifier id, EditableComponentType type) {
		SwitchyComponentType.TextProvider<T> provider = type.preview == null ? null :  (SwitchyComponentType.TextProvider<T>) SwitchyComponentTypes.TEXT_PROVIDERS.get(Identifier.tryParse(type.preview));
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
		types.register(id, codec, b -> b
			.nbtSwitcher(type.path)
			.textProvider(v -> Text.empty().append(Text.literal(Objects.requireNonNullElse(type.prefix, "")).formatted(Formatting.GRAY)).append(provider != null ? provider.toText(v) : Text.of(Objects.toString(v))))
			.argumentEditor(editor)
			.emptyChecker(checker)
			.group(type.group == null ? null : Identifier.tryParse(type.group))
			.initializer(initializer) // overrides switcher
		);
	}

	@Override
	public Identifier getFabricId() {
		return Switchy.id(PATH);
	}
}

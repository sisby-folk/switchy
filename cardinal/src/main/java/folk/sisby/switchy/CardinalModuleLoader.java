package folk.sisby.switchy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.modules.CardinalSerializerCompat;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.resource.loader.api.reloader.IdentifiableResourceReloader;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * A resource loader responsible for creating data-driven {@link SwitchyModule}s for swapping Cardinal Components API entity component data.
 * Will load json files in {@code data/{namespace}/switchy_cardinal/{path}.json} into modules with {@code namespace:path} IDs in the below format:
 * {@code
 * {
 * "default": boolean,
 * "editable": SwitchyModuleEditable,
 * "ifModsLoaded": ["mod-id"],
 * "components": ["component-id"]
 * }
 * }
 *
 * @author Sisby folk
 * @see CardinalSerializerCompat
 * @since 1.8.0
 */
public class CardinalModuleLoader extends JsonDataLoader implements IdentifiableResourceReloader {
	/**
	 * The global instance for this resource loader.
	 */
	public static final CardinalModuleLoader INSTANCE = new CardinalModuleLoader(new Gson());
	private static final Identifier ID = new Identifier(Switchy.ID, "switchy_cardinal");
	private static final String KEY_DEFAULT = "default";
	private static final String KEY_EDITABLE = "editable";
	private static final String KEY_IF_MODS_LOADED = "ifModsLoaded";
	private static final String KEY_COMPONENTS = "components";

	CardinalModuleLoader(Gson gson) {
		super(gson, "switchy_cca_modules");
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		prepared.forEach((moduleId, contents) -> {
			if (SwitchyModuleRegistry.containsModule(moduleId)) {
				return;
			}
			JsonObject componentOptions = contents.getAsJsonObject();
			if (!componentOptions.has(KEY_DEFAULT) || !componentOptions.has(KEY_EDITABLE) || !componentOptions.has(KEY_COMPONENTS)) {
				Switchy.LOGGER.warn("[Switchy] CCA module '{}' is missing options, skipping...", moduleId);
				return;
			}
			if (componentOptions.has(KEY_IF_MODS_LOADED) && !StreamSupport.stream(componentOptions.get(KEY_IF_MODS_LOADED).getAsJsonArray().spliterator(), true).map(JsonElement::getAsString).allMatch(QuiltLoader::isModLoaded)) {
				return;
			}
			try {
				SwitchyModuleEditable componentEditable = SwitchyModuleEditable.valueOf(componentOptions.get(KEY_EDITABLE).getAsString());
				boolean componentDefault = componentOptions.get(KEY_DEFAULT).getAsBoolean();
				Set<Identifier> componentIds = new HashSet<>();
				for (JsonElement componentEntry : componentOptions.get(KEY_COMPONENTS).getAsJsonArray()) {
					Identifier componentId = Identifier.tryParse(componentEntry.getAsString());
					if (componentId == null) {
						Switchy.LOGGER.warn("[Switchy] Cardinal component '{}' from module {} is not a valid identifier, skipping...", componentEntry.getAsString(), moduleId);
						componentIds.clear();
						break;
					}
					componentIds.add(componentId);
				}
				if (!componentIds.isEmpty()) {
					try {
						CardinalSerializerCompat.register(moduleId, componentIds, componentDefault, componentEditable);
					} catch (IllegalStateException ignored) {
						Switchy.LOGGER.warn("[Switchy] CCA module {} tried to register a component that already has a module!, skipping...", moduleId);
					}
				}
			} catch (UnsupportedOperationException e) {
				Switchy.LOGGER.warn("[Switchy] CCA module '{}' has non-boolean options, skipping...", moduleId);
			} catch (IllegalArgumentException e) {
				Switchy.LOGGER.warn("[Switchy] CCA module '{}' has invalid editable option, skipping...", moduleId);
			}
		});
	}

	@Override
	public @NotNull Identifier getQuiltId() {
		return ID;
	}
}

package folk.sisby.switchy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
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

public class CardinalModuleLoader extends JsonDataLoader implements IdentifiableResourceReloader {
	private static final Identifier ID = new Identifier(Switchy.ID, "switchy_cca_modules");

	private static final String KEY_DEFAULT = "default";
	private static final String KEY_IMPORTABLE = "importable";
	private static final String KEY_IF_MODS_LOADED = "ifModsLoaded";
	private static final String KEY_COMPONENTS = "components";

	public static final CardinalModuleLoader INSTANCE = new CardinalModuleLoader(new Gson());

	public CardinalModuleLoader(Gson gson) {
		super(gson, "switchy_cca_modules");
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		for (Map.Entry<Identifier, JsonElement> file : prepared.entrySet()) {
			Identifier moduleId = file.getKey(); // namespace:filename
			if (SwitchyModules.MODULE_SUPPLIERS.containsKey(moduleId)) {
				continue;
			}

			JsonObject componentOptions = file.getValue().getAsJsonObject();

			if (!componentOptions.has(KEY_DEFAULT)|| !componentOptions.has(KEY_IMPORTABLE) || !componentOptions.has(KEY_COMPONENTS)) {
				Switchy.LOGGER.warn("Switchy: CCA module '{}' is missing options, skipping...", file.getKey());
				continue;
			}
			if (componentOptions.has(KEY_IF_MODS_LOADED) && StreamSupport.stream(componentOptions.get(KEY_IF_MODS_LOADED).getAsJsonArray().spliterator(), true).map(JsonElement::getAsString).noneMatch(QuiltLoader::isModLoaded)) {
				continue;
			}
			try {
				SwitchyModuleEditable componentImportable = SwitchyModuleEditable.valueOf(componentOptions.get(KEY_IMPORTABLE).getAsString());
				boolean componentDefault = componentOptions.get(KEY_DEFAULT).getAsBoolean();
				Set<Identifier> componentIds = new HashSet<>();
				for (JsonElement componentEntry : componentOptions.get(KEY_COMPONENTS).getAsJsonArray()) {
					Identifier componentId = Identifier.tryParse(componentEntry.getAsString());
					if (componentId == null) {
						Switchy.LOGGER.warn("Switchy: Cardinal component '{}' from module {} is not a valid identifier, skipping...", componentEntry.getAsString(), file.getKey());
						componentIds.clear();
						break;
					}
					componentIds.add(componentId);
				}
				if (!componentIds.isEmpty()) {
					try {
						CardinalSerializerCompat.register(moduleId, componentIds, componentDefault, componentImportable);
					} catch (IllegalStateException ignored) {
						Switchy.LOGGER.warn("Switchy: CCA module {} tried to register a component that already has a module!, skipping...", file.getKey());
					}
				}
			} catch (UnsupportedOperationException e) {
				Switchy.LOGGER.warn("Switchy: CCA module '{}' has non-boolean options, skipping...", file.getKey());
			} catch (IllegalArgumentException e) {
				Switchy.LOGGER.warn("Switchy: CCA module '{}' has invalid importable option, skipping...", file.getKey());
			}
		}
	}

	@Override
	public @NotNull Identifier getQuiltId() {
		return ID;
	}
}

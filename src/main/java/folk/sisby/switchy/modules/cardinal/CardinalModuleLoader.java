package folk.sisby.switchy.modules.cardinal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.modules.CardinalSerializerCompat;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.qsl.resource.loader.api.reloader.IdentifiableResourceReloader;

import java.util.Map;

public class CardinalModuleLoader extends JsonDataLoader implements IdentifiableResourceReloader {
	private static final Identifier ID = new Identifier(Switchy.ID, "switchy_cca_modules");

	public static final CardinalModuleLoader INSTANCE = new CardinalModuleLoader(new Gson());

	public CardinalModuleLoader(Gson gson) {
		super(gson, "switchy_cca_modules");
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		for (Map.Entry<Identifier, JsonElement> file : prepared.entrySet()) {
			for (Map.Entry<String, JsonElement> entry : file.getValue().getAsJsonObject().entrySet()) {
				Identifier componentId = Identifier.tryParse(entry.getKey());
				if (componentId == null) {
					Switchy.LOGGER.warn("Switchy: Cardinal component '{}' is not a valid identifier, skipping...", entry.getKey());
					continue;
				}
				Identifier moduleId = new Identifier(Switchy.ID + "_cardinal", componentId.toUnderscoreSeparatedString());
				if (Switchy.COMPAT_REGISTRY.containsKey(moduleId)) {
					continue;
				}
				JsonObject componentOptions = entry.getValue().getAsJsonObject();
				if (!componentOptions.has("default")|| !componentOptions.has("importable")) {
					Switchy.LOGGER.warn("Switchy: Cardinal component '{}' is missing options, skipping...", entry.getKey());
					continue;
				}
				try {
					ModuleImportable componentImportable = ModuleImportable.valueOf(componentOptions.get("default").getAsString());
					boolean componentDefault = componentOptions.get("default").getAsBoolean();
					CardinalSerializerCompat.tryRegister(moduleId, componentId, componentDefault, componentImportable);
				} catch (UnsupportedOperationException e) {
					Switchy.LOGGER.warn("Switchy: Cardinal component '{}' has non-boolean options, skipping...", entry.getKey());
				} catch (IllegalArgumentException e) {
					Switchy.LOGGER.warn("Switchy: Cardinal component '{}' has invalid importable option, skipping...", entry.getKey());
				}
			}
		}
	}

	@Override
	public @NotNull Identifier getQuiltId() {
		return ID;
	}
}
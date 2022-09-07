package folk.sisby.switchy.modules.modfest;

import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.PresetModuleRegistry;
import folk.sisby.switchy.api.modules.CardinalSerializerCompat;
import net.minecraft.util.Identifier;

public class ModfestCardinalsCompat {
	// Runs on touch() - but only once.
	static {
		// Arcpocalypse
		Identifier arcModuleId = new Identifier(Switchy.ID, "arcpocalypse");
		Identifier arcId = new Identifier("arcpocalypse", "nekoarc");
		if (ComponentRegistry.stream().anyMatch(key -> key.getId() == arcId)) {
			PresetModuleRegistry.registerModule(arcModuleId, () -> new CardinalSerializerCompat<>(arcModuleId, ComponentRegistry.get(arcId), (a, b) -> {}, (a, b) -> {}, false));
		}

		// Lanyard
		Identifier lanyardModuleId = new Identifier(Switchy.ID, "lanyard");
		Identifier lanyardId = new Identifier("lanyard", "profiles");
		if (ComponentRegistry.stream().anyMatch(key -> key.getId() == lanyardId)) {
			PresetModuleRegistry.registerModule(lanyardModuleId, () -> new CardinalSerializerCompat<>(lanyardModuleId, ComponentRegistry.get(lanyardId), (a, b) -> {}, (a, b) -> {}, false));
		}


	}

	public static void touch() {
	}
}

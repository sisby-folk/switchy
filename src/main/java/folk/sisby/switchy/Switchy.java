package folk.sisby.switchy;

import folk.sisby.switchy.compat.*;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Switchy implements ModInitializer {

	public static final String ID = "switchy";

	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	static final Map<Identifier, Supplier<? extends PresetCompatModule>> COMPAT_REGISTRY = new HashMap<>();

	public static void registerModule(Identifier moduleId, Supplier<? extends PresetCompatModule> moduleConstructor) {
		if (!COMPAT_REGISTRY.containsKey(moduleId)) {
			COMPAT_REGISTRY.put(moduleId, moduleConstructor);
			LOGGER.info("Switchy: Registered module " + moduleId);
		} else {
			LOGGER.error("Switchy: Couldn't register module " + moduleId + "as it was already loaded");
		}
	}

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyCommands.InitializeCommands();

		if (QuiltLoader.isModLoaded("drogtor")) DrogtorCompat.touch();
		if (QuiltLoader.isModLoaded("playerpronouns")) PlayerPronounsCompat.touch();
		if (QuiltLoader.isModLoaded("fabrictailor")) FabricTailorCompat.touch();
		if (QuiltLoader.isModLoaded("origins")) OriginsCompat.touch();
		InventoryCompat.touch();

		LOGGER.info("Switchy: Initialized! Registered Base Modules: " + COMPAT_REGISTRY.keySet());
	}
}

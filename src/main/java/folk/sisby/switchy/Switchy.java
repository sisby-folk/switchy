package folk.sisby.switchy;

import folk.sisby.switchy.compat.*;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Switchy implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Switchy");
	public static final List<PresetCompat> COMPAT_MODULES = new ArrayList<>();

	@Override
	public void onInitialize(ModContainer mod) {
		SwitchyCommands.InitializeCommands();

		if (QuiltLoader.isModLoaded("drogtor")) {
			COMPAT_MODULES.add(new DrogtorCompat());
		}
		if (QuiltLoader.isModLoaded("playerpronouns")) {
			COMPAT_MODULES.add(new PlayerPronounsCompat());
		}
		if (QuiltLoader.isModLoaded("fabrictailor")) {
			COMPAT_MODULES.add(new FabricTailorCompat());
		}
		if (QuiltLoader.isModLoaded("origins")) {
			COMPAT_MODULES.add(new OriginsCompat());
		}

		LOGGER.info("Switchy Initialized! Compatibility enabled for: "+ COMPAT_MODULES.stream().map(PresetCompat::getKey).collect(Collectors.joining(", ")));
	}

}

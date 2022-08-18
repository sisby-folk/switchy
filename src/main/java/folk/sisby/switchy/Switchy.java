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
			DrogtorCompat.register();
		}
		if (QuiltLoader.isModLoaded("playerpronouns")) {
			PlayerPronounsCompat.register();
		}
		if (QuiltLoader.isModLoaded("fabrictailor")) {
			FabricTailorCompat.register();
		}
		if (QuiltLoader.isModLoaded("origins")) {
			OriginsCompat.register();
		}

		LOGGER.info("Switchy Initialized! Compatibility enabled for: "+ COMPAT_MODULES.stream().map(PresetCompat::getKey).collect(Collectors.joining(", ")));
	}

}

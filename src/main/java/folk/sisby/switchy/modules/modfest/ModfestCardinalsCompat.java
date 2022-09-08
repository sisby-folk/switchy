package folk.sisby.switchy.modules.modfest;

import org.quiltmc.loader.api.QuiltLoader;

public class ModfestCardinalsCompat {
	// Runs on touch() - but only once.
	static {
		if (QuiltLoader.isModLoaded("arcpocalypse")) ArcpocalypseCompat.touch();
		if (QuiltLoader.isModLoaded("lanyard")) LanyardCompat.touch();
	}

	public static void touch() {
	}
}

package folk.sisby.switchy.modules.modfest;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.modules.CardinalAutoSyncCompat;
import net.minecraft.util.Identifier;

public class ModfestCardinalsCompat {
	// Runs on touch() - but only once.
	static {
		CardinalAutoSyncCompat.tryRegister(new Identifier(Switchy.ID, "arcpocalypse"), new Identifier("arcpocalypse", "nekoarc"), true);
		CardinalAutoSyncCompat.tryRegister(new Identifier(Switchy.ID, "lanyard"), new Identifier("lanyard", "profiles"), true);
	}

	public static void touch() {
	}
}

package folk.sisby.switchy.modules.modfest;

import arathain.arcpocalypse.ArcpocalypseComponents;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.modules.CardinalSerializerCompat;
import gay.pyrrha.lanyard.component.ModComponents;
import net.minecraft.util.Identifier;

public class ArcpocalypseCompat {
	// Runs on touch() - but only once.
	static {
		CardinalSerializerCompat.tryRegister(new Identifier(Switchy.ID, "arcpocalypse"), ArcpocalypseComponents.ARC_COMPONENT.getId(), true);
	}

	public static void touch() {
	}
}

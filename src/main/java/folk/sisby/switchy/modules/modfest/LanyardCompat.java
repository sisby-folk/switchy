package folk.sisby.switchy.modules.modfest;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.modules.CardinalSerializerCompat;
import gay.pyrrha.lanyard.component.ModComponents;
import net.minecraft.util.Identifier;

public class LanyardCompat {
	// Runs on touch() - but only once.
	static {
		CardinalSerializerCompat.tryRegister(new Identifier(Switchy.ID, "lanyard"), ModComponents.PLAYER_PROFILES.getId(), true);
	}

	public static void touch() {
	}
}

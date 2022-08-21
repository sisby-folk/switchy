package folk.sisby.switchy.api;

import folk.sisby.switchy.Switchy;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class SwitchyModuleRegistry {
	public static void registerModule(Identifier moduleId, Supplier<? extends PresetCompatModule> moduleConstructor) {
		Switchy.registerModule(moduleId, moduleConstructor);
	}
}

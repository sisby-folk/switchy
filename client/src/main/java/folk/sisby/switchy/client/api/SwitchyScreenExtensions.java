package folk.sisby.switchy.client.api;

import folk.sisby.switchy.client.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.client.screen.SwitchScreen.ComponentPosition;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.util.Identifier;

import java.util.function.Function;

import static folk.sisby.switchy.client.screen.SwitchScreen.registerPresetDisplayComponent;

public class SwitchyScreenExtensions {
	public static void registerQuickSwitchDisplayComponent(Identifier id, ComponentPosition pos, Function<SwitchyDisplayPreset, Component> componentFunction) {
		registerPresetDisplayComponent(id, pos, componentFunction);
	}
}

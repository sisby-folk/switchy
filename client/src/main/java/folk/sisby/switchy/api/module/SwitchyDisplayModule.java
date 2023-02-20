package folk.sisby.switchy.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.screen.SwitchScreen.ComponentPosition;
import io.wispforest.owo.ui.core.Component;

public interface SwitchyDisplayModule<Module extends SwitchyModuleData> extends SwitchyDisplayModuleData<Module> {
	/**
	 * Only runs on the client
	 * Should be using transformed data if needed.
	 */
	Pair<Component, ComponentPosition> getDisplayComponent();
}

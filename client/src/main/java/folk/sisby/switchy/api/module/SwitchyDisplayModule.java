package folk.sisby.switchy.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.screen.SwitchScreen.ComponentPosition;
import io.wispforest.owo.ui.core.Component;

public interface SwitchyDisplayModule<Module extends SwitchyModuleData> extends SwitchyModuleData {
	/**
	 * Only runs on the server
	 * Any data you need to transform using server mods before serializing - do it here.
	 */
	void fillFromData(Module module);

	/**
	 * Only runs on the client
	 * Should be using transformed data if needed.
	 */
	Pair<Component, ComponentPosition> getDisplayComponent();
}

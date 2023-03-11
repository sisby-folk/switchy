package folk.sisby.switchy.ui.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import io.wispforest.owo.ui.core.Component;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * An extension for client modules that can be rendered on the Switchy UI switch screen.
 *
 * @author Sisby folk
 * @see folk.sisby.switchy.client.api.module.SwitchyClientModule
 * @since 2.0.0
 */
@ClientOnly
public interface SwitchyUIModule {
	/**
	 * Gets the UI component for previewing the module, and where to display it.
	 *
	 * @param presetName the name of the preset being previewed.
	 * @return a renderable component, and the position on the preset preview it should be placed.
	 * Null if no component should be added.
	 */
	@Nullable Pair<Component, SwitchyUIPosition> getPreviewComponent(String presetName);
}

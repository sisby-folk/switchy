package folk.sisby.switchy.ui.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.ui.api.SwitchySwitchScreenPosition;
import io.wispforest.owo.ui.core.Component;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * A type of client module that can be rendered on the Switchy UI switch screen.
 *
 * @author Sisby folk
 * @see folk.sisby.switchy.client.api.module.SwitchyClientModule
 * @since 2.0.0
 */
@ClientOnly
public interface SwitchyDisplayModule {
	/**
	 * @return a renderable component, and the position on the preset preview it should be placed.
	 */
	@Nullable Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent();
}

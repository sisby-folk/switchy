package folk.sisby.switchy.ui.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.module.SwitchyModuleClientable;
import folk.sisby.switchy.ui.api.SwitchySwitchScreenPosition;
import io.wispforest.owo.ui.core.Component;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * Holds a client-appropriate representation of the contents of a Switchy Module.
 * The represented module must implement {@link SwitchyModuleClientable}.
 *
 * @author Sisby folk
 * @see SwitchySerializable
 * @see folk.sisby.switchy.api.module.SwitchyModule
 * @since 2.0.0
 */
@ClientOnly
public interface SwitchyDisplayModule extends SwitchySerializable {
	/**
	 * @return a renderable component, and the position on the preset preview it should be placed.
	 */
	@Nullable Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent();
}

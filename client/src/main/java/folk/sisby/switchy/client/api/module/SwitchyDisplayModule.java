package folk.sisby.switchy.client.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import io.wispforest.owo.ui.core.Component;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * @author Sisby folk
 * @since 2.0.0
 * @see SwitchySerializable
 * @see folk.sisby.switchy.api.module.SwitchyModule
 * Holds a client-appropriate representation of the contents of a {@link folk.sisby.switchy.api.module.SwitchyModule}.
 * Capable of being rendered to the {@link folk.sisby.switchy.client.screen.SwitchScreen}
 * The {@link folk.sisby.switchy.api.module.SwitchyModule} this represents must implement {@link folk.sisby.switchy.api.module.SwitchyModuleDisplayable}
 */
@ClientOnly
public interface SwitchyDisplayModule extends SwitchySerializable {
	/**
	 * @return a render-able component, and the position on the preset preview it should be placed
	 */
	@Nullable Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent();
}

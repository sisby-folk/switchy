package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import folk.sisby.switchy.modules.DrogtorCompatData;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * The client-displayable variant of a module that switches nicknames from unascribed's Drogtor The Nickinator.
 *
 * @author Sisby folk
 * @see SwitchyDisplayModule
 * @see folk.sisby.switchy.modules.DrogtorCompat
 * @since 2.0.0
 */
@ClientOnly
public class DrogtorCompatDisplay extends DrogtorCompatData implements SwitchyDisplayModule {
	static {
		SwitchyDisplayModuleRegistry.registerModule(DrogtorCompatDisplay.ID, DrogtorCompatDisplay::new);
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent() {
		Text text = getText();
		if (text == null) return null;
		return Pair.of(Components.label(text), SwitchySwitchScreenPosition.LEFT);
	}
}

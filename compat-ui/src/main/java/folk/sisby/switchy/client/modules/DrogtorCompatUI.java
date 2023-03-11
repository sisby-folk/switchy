package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.modules.DrogtorCompatData;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import folk.sisby.switchy.ui.api.module.SwitchyUIModule;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * The client-displayable variant of a module that switches nicknames from unascribed's Drogtor The Nickinator.
 *
 * @author Sisby folk
 * @see SwitchyUIModule
 * @see folk.sisby.switchy.modules.DrogtorCompat
 * @since 2.0.0
 */
@ClientOnly
public class DrogtorCompatUI extends DrogtorCompatData implements SwitchyClientModule, SwitchyUIModule {
	static {
		SwitchyClientModuleRegistry.registerModule(DrogtorCompatUI.ID, DrogtorCompatUI::new);
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public Pair<Component, SwitchyUIPosition> getPreviewComponent(String presetName) {
		Text text = getText();
		if (text == null) return null;
		return Pair.of(Components.label(text), SwitchyUIPosition.LEFT);
	}
}

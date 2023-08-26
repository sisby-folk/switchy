package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.modules.DrogtorModuleData;
import folk.sisby.switchy.modules.DrogtorModule;
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
 * @see DrogtorModule
 * @since 2.0.0
 */
@ClientOnly
public class DrogtorClientModule extends DrogtorModuleData implements SwitchyClientModule, SwitchyUIModule {
	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyClientModuleRegistry.registerModule(DrogtorClientModule.ID, DrogtorClientModule::new);
	}

	@Override
	public Pair<Component, SwitchyUIPosition> getPreviewComponent(String presetName) {
		Text text = getText();
		if (text == null) return null;
		return Pair.of(Components.label(text), SwitchyUIPosition.LEFT);
	}
}

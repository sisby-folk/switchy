package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import folk.sisby.switchy.modules.DrogtorCompatData;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * @author Sisby folk
 * @since 2.0.0
 * @see SwitchyDisplayModule
 * @see folk.sisby.switchy.modules.DrogtorCompat
 * The client-displayable variant of a module that switches nicknames from unascribed's Drogtor The Nickinator
 */
@ClientOnly
public class DrogtorCompatDisplay extends DrogtorCompatData implements SwitchyDisplayModule {
	@Override
	public Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent() {
		if (nickname == null) return null;
		Style style = Style.EMPTY;
		if (namecolor != null) style = style.withColor(namecolor);
		if (bio != null) style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(bio)));
		return Pair.of(Components.label(Text.literal(nickname).setStyle(style)), SwitchySwitchScreenPosition.LEFT);
	}

	/**
	 * Executes {@code static} the first time it's invoked
	 */
	public static void touch() {}

	static {
		SwitchyDisplayModuleRegistry.registerModule(DrogtorCompatDisplay.ID, DrogtorCompatDisplay::new);
	}
}

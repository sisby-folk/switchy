package folk.sisby.switchy.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.client.screen.SwitchScreen.ComponentPosition;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class DrogtorCompatDisplay extends DrogtorCompatData implements SwitchyDisplayModule<DrogtorCompatData> {
	@Override
	public void fillFromData(DrogtorCompatData module) {
		this.fillFromNbt(module.toNbt());
	}

	@Override
	public Pair<Component, ComponentPosition> getDisplayComponent() {
		if (nickname == null) return null;
		Style style = Style.EMPTY;
		if (namecolor != null) style = style.withColor(namecolor);
		if (bio != null) style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(bio)));
		return Pair.of(Components.label(Text.literal(nickname).setStyle(style)), ComponentPosition.LEFT);
	}

	public static void touch() {
		SwitchyClientModuleRegistry.registerModule(DrogtorCompatDisplay.ID, DrogtorCompatDisplay::new);
	}
}

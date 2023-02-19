package folk.sisby.switchy.modules;

import folk.sisby.switchy.client.api.SwitchyScreenExtensions;
import folk.sisby.switchy.client.screen.SwitchScreen.ComponentPosition;
import io.wispforest.owo.ui.component.Components;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class DrogtorCompatClient {
	public static final Identifier ID = new Identifier("switchy",  "drogtor");

	public static final String KEY_NICKNAME = "nickname";
	public static final String KEY_NAME_COLOR = "nameColor";
	public static final String KEY_BIO = "bio";


	public static void touch() {}

	static {
		SwitchyScreenExtensions.registerQuickSwitchDisplayComponent(ID, ComponentPosition.RIGHT, displayPreset -> {
			if (!displayPreset.modules.containsKey(ID)) return null;
			NbtCompound nbt = displayPreset.modules.get(ID);
			if (!nbt.contains(KEY_NICKNAME)) return null;
			Style style = Style.EMPTY;
			if (nbt.contains(KEY_NAME_COLOR)) style = style.withColor(Formatting.byName(nbt.getString(KEY_NAME_COLOR)));
			if (nbt.contains(KEY_BIO)) style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(nbt.getString(KEY_BIO))));
			return Components.label(Text.literal(nbt.getString(KEY_NICKNAME)).setStyle(style));
		});
	}
}

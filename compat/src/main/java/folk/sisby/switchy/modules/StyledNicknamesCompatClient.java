package folk.sisby.switchy.modules;

import folk.sisby.switchy.client.api.SwitchyScreenExtensions;
import folk.sisby.switchy.client.screen.SwitchScreen;
import io.wispforest.owo.ui.component.Components;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class StyledNicknamesCompatClient {
	public static final Identifier ID = new Identifier("switchy",  "styled_nicknames");

	public static final String KEY_NICKNAME = "styled_nickname";
	public static final String KEY_COLOR = "nameColor";

	public static void touch() {}

	static {
		SwitchyScreenExtensions.registerQuickSwitchDisplayComponent(ID, SwitchScreen.ComponentPosition.LEFT, displayPreset -> {
			if (!displayPreset.modules.containsKey(ID)) return null;
			NbtCompound nbt = displayPreset.modules.get(ID);
			if (!nbt.contains(KEY_NICKNAME)) return null;
			Style style = Style.EMPTY;
			if (!nbt.contains(KEY_COLOR)) style.withColor(Formatting.byName(nbt.getString(KEY_COLOR)));
			return Components.label(Text.literal(nbt.getString(KEY_NICKNAME)).setStyle(style));
		});
	}
}

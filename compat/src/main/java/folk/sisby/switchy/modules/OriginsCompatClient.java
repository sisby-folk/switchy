package folk.sisby.switchy.modules;

import folk.sisby.switchy.client.api.SwitchyScreenExtensions;
import folk.sisby.switchy.client.screen.SwitchScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.HorizontalFlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static io.github.apace100.origins.registry.ModItems.ORB_OF_ORIGIN;

public class OriginsCompatClient {
	public static final Identifier ID = new Identifier("switchy",  "origins");

	public static final String KEY_ORIGINS_LIST = "OriginLayers";

	public static void touch() {}

	static {
		SwitchyScreenExtensions.registerQuickSwitchDisplayComponent(ID, SwitchScreen.ComponentPosition.LEFT, displayPreset -> {
			if (!displayPreset.modules.containsKey(ID)) return null;
			NbtCompound nbt = displayPreset.modules.get(ID);
			if (!nbt.contains(KEY_ORIGINS_LIST, NbtElement.LIST_TYPE)) return null;
			List<String> originNames = new ArrayList<>();
			nbt.getList(KEY_ORIGINS_LIST, NbtElement.COMPOUND_TYPE).forEach((nbtElement -> {
				if (nbtElement instanceof NbtCompound c) {
					if (c.contains("Origin", NbtElement.STRING_TYPE)) {
						originNames.add(c.getString("Origin"));
					}
				}
			}));

			if (originNames.isEmpty()) return null;

			HorizontalFlowLayout originsFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			originsFlow.verticalAlignment(VerticalAlignment.CENTER);
			originsFlow.child(Components.item(ORB_OF_ORIGIN.getDefaultStack()));
			originsFlow.child(Components.label(Text.literal(String.join(" | ", originNames)).setStyle(Style.EMPTY.withColor(Formatting.GRAY))));

			return originsFlow;
		});
	}
}

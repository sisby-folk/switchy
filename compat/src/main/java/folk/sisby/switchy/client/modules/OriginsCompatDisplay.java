package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.HorizontalFlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.apace100.origins.registry.ModItems.ORB_OF_ORIGIN;

public class OriginsCompatDisplay implements SwitchyDisplayModule {
	public static final Identifier ID = new Identifier("switchy",  "origins");
	public Map<String, Identifier> origins;

	public static final String KEY_ORIGINS_LIST = "OriginLayers";
	public static final String KEY_LAYER = "Layer";
	public static final String KEY_ORIGIN = "Origin";

	@Override
	public Pair<Component, SwitchScreenPosition> getDisplayComponent() {
		if (origins == null || origins.isEmpty()) return null;

		HorizontalFlowLayout originsFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		originsFlow.verticalAlignment(VerticalAlignment.CENTER);
		originsFlow.child(Components.item(ORB_OF_ORIGIN.getDefaultStack()));
		originsFlow.child(Components.label(Text.literal(
				origins.values().stream().map(Identifier::getPath).collect(Collectors.joining(" | ")))
				.setStyle(Style.EMPTY.withColor(Formatting.GRAY))));

		return Pair.of(originsFlow, SwitchScreenPosition.LEFT);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		NbtList originLayerList = new NbtList();
		if (this.origins != null) {
			origins.forEach((key, value) -> {
				NbtCompound layerTag = new NbtCompound();
				layerTag.putString(KEY_LAYER, key);
				layerTag.putString(KEY_ORIGIN, value.toString());
				originLayerList.add(layerTag);
			});
		}
		outNbt.put(KEY_ORIGINS_LIST, originLayerList);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.origins = new HashMap<>();
		if (nbt.contains(KEY_ORIGINS_LIST, NbtElement.LIST_TYPE)) {
			NbtList originLayerList = nbt.getList(KEY_ORIGINS_LIST, NbtElement.COMPOUND_TYPE);
			for (NbtElement layerElement : originLayerList) {
				if (layerElement instanceof NbtCompound layerCompound) {
					this.origins.put(layerCompound.getString(KEY_LAYER), new Identifier(layerCompound.getString(KEY_ORIGIN)));
				}
			}
		}
	}

	public static void touch() {}

	static {
		SwitchyDisplayModuleRegistry.registerModule(ID, OriginsCompatDisplay::new);
	}
}

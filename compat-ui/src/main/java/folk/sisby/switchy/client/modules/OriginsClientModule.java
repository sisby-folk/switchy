package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.modules.OriginsModule;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import folk.sisby.switchy.ui.api.module.SwitchyUIModule;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.*;

import static io.github.apace100.origins.registry.ModItems.ORB_OF_ORIGIN;

/**
 * The client-displayable variant of a module that switches layered Origins from Apace's Origins.
 *
 * @author Sisby folk
 * @see SwitchyUIModule
 * @see OriginsModule
 * @since 2.0.0
 */
@ClientOnly
public class OriginsClientModule implements SwitchyClientModule, SwitchyUIModule {
	/**
	 * Identifier for this module.
	 * Must match {@link OriginsModule}.
	 */
	public static final Identifier ID = new Identifier("switchy", "origins");

	/**
	 * The NBT key where the list of origins is stored.
	 * Must match {@link OriginsModule#toClientNbt()}.
	 */
	public static final String KEY_ORIGINS_LIST = "OriginLayers";
	/**
	 * The NBT key where the layer is stored in each list item.
	 * Must match {@link OriginsModule#toClientNbt()}.
	 */
	public static final String KEY_LAYER = "Layer";
	/**
	 * The NBT key where the origin ID is stored in each list item.
	 * Must match {@link OriginsModule#toClientNbt()}.
	 */
	public static final String KEY_ORIGIN = "Origin";

	static {
		SwitchyClientModuleRegistry.registerModule(ID, OriginsClientModule::new);
	}

	/**
	 * The origin identifiers per layer.
	 */
	public Map<OriginLayer, Origin> origins;

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public Pair<Component, SwitchyUIPosition> getPreviewComponent(String presetName) {
		if (origins == null || origins.isEmpty()) return null;

		FlowLayout originsFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		originsFlow.verticalAlignment(VerticalAlignment.CENTER);
		originsFlow.child(Components.item(ORB_OF_ORIGIN.getDefaultStack()).margins(Insets.right(2)));
		List<String> names = new ArrayList<>(origins.values().stream().map(Origin::getName).map(Text::getString).toList());
		Collections.reverse(names);
		originsFlow.child(Components.label(Text.literal(String.join(" | ", names)).setStyle(Style.EMPTY.withColor(Formatting.GRAY))));

		return Pair.of(originsFlow, SwitchyUIPosition.LEFT);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		NbtList originLayerList = new NbtList();
		if (origins != null) {
			origins.forEach((key, value) -> {
				NbtCompound layerTag = new NbtCompound();
				layerTag.putString(KEY_LAYER, key.getIdentifier().toString());
				layerTag.putString(KEY_ORIGIN, value.toString());
				originLayerList.add(layerTag);
			});
		}
		outNbt.put(KEY_ORIGINS_LIST, originLayerList);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		origins = new HashMap<>();
		if (nbt.contains(KEY_ORIGINS_LIST, NbtElement.LIST_TYPE)) {
			NbtList originLayerList = nbt.getList(KEY_ORIGINS_LIST, NbtElement.COMPOUND_TYPE);
			for (NbtElement layerElement : originLayerList) {
				if (layerElement instanceof NbtCompound layerCompound) {
					origins.put(OriginLayers.getLayer(Identifier.tryParse(layerCompound.getString(KEY_LAYER))), OriginRegistry.get(Identifier.tryParse(layerCompound.getString(KEY_ORIGIN))));
				}
			}
		}
	}
}

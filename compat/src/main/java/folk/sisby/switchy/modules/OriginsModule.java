package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.*;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches layered Origins from Apace's Origins.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.0.0
 */
public class OriginsModule implements SwitchyModule, SwitchyModuleTransferable {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "origins");

	/**
	 * The NBT key where the list of origins is stored.
	 */
	public static final String KEY_ORIGINS_LIST = "OriginLayers";
	/**
	 * The NBT key where the layer is stored in each list item.
	 */
	public static final String KEY_LAYER = "Layer";
	/**
	 * The NBT key where the origin ID is stored in each list item.
	 */
	public static final String KEY_ORIGIN = "Origin";

	static {
		SwitchyModuleRegistry.registerModule(ID, OriginsModule::new, new SwitchyModuleInfo(
						true,
						SwitchyModuleEditable.ALLOWED,
						translatable("switchy.compat.module.origins.description")
				)
						.withDescriptionWhenEnabled(translatable("switchy.compat.module.origins.enabled"))
						.withDescriptionWhenDisabled(translatable("switchy.compat.module.origins.disabled"))
						.withDeletionWarning(translatable("switchy.compat.module.origins.warning"))
		);
	}

	/**
	 * The origins per layer.
	 */
	@Nullable public Map<OriginLayer, Origin> origins;

	private static void setOrigin(ServerPlayerEntity player, OriginLayer layer, Origin origin) {
		OriginComponent component = ModComponents.ORIGIN.get(player);
		component.setOrigin(layer, origin);
		OriginComponent.sync(player);
		boolean hadOriginBefore = component.hadOriginBefore();
		OriginComponent.partialOnChosen(player, hadOriginBefore, origin);
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		OriginComponent originComponent = ModComponents.ORIGIN.get(player);
		origins = new HashMap<>(originComponent.getOrigins());
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		if (origins != null) {
			for (OriginLayer layer : origins.keySet()) {
				setOrigin(player, layer, origins.get(layer));
			}
		}
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		// From Origins PlayerOriginComponent
		NbtList originLayerList = new NbtList();
		if (origins != null) {
			origins.forEach((key, value) -> {
				NbtCompound layerTag = new NbtCompound();
				layerTag.putString(KEY_LAYER, key.getIdentifier().toString());
				layerTag.putString(KEY_ORIGIN, value.getIdentifier().toString());
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
					String layerId = layerCompound.getString(KEY_LAYER);
					String originId = layerCompound.getString(KEY_ORIGIN);
					try {
						OriginLayer layer = OriginLayers.getLayer(Identifier.tryParse(layerId));
						Origin origin = OriginRegistry.get(Identifier.tryParse(originId));
						origins.put(layer, origin);
					} catch (IllegalArgumentException originGetEx) {
						Switchy.LOGGER.warn("[Switchy] Failed to load preset origin with layer {} and origin {}. Exception: {}", layerId, originId, originGetEx);
					}
				}
			}
		}
	}
}

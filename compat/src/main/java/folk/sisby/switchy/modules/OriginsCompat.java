package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
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

public class OriginsCompat implements SwitchyModule {
	public static final Identifier ID = new Identifier("switchy",  "origins");

	public static final String KEY_ORIGINS_LIST = "OriginLayers";
	public static final String KEY_LAYER = "Layer";
	public static final String KEY_ORIGIN = "Origin";

	// Overwritten on save when null
	@Nullable public Map<OriginLayer, Origin> origins;

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		OriginComponent originComponent = ModComponents.ORIGIN.get(player);
		this.origins = new HashMap<>(originComponent.getOrigins());
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		if (this.origins != null) {
			for (OriginLayer layer : this.origins.keySet()) {
				setOrigin(player, layer, this.origins.get(layer));
			}
		}
	}

	private static void setOrigin(ServerPlayerEntity player, OriginLayer layer, Origin origin) {
		OriginComponent component = ModComponents.ORIGIN.get(player);
		component.setOrigin(layer, origin);
		OriginComponent.sync(player);
		boolean hadOriginBefore = component.hadOriginBefore();
		OriginComponent.partialOnChosen(player, hadOriginBefore, origin);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		// From Origins PlayerOriginComponent
		NbtList originLayerList = new NbtList();
		if (this.origins != null) {
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
		this.origins = new HashMap<>();
		if (nbt.contains(KEY_ORIGINS_LIST, NbtElement.LIST_TYPE)) {
			NbtList originLayerList = nbt.getList(KEY_ORIGINS_LIST, NbtElement.COMPOUND_TYPE);
			for (NbtElement layerElement : originLayerList) {
				if (layerElement instanceof NbtCompound layerCompound) {
					String layerId = layerCompound.getString(KEY_LAYER);
					String originId = layerCompound.getString(KEY_ORIGIN);
					try {
						OriginLayer layer = OriginLayers.getLayer(Identifier.tryParse(layerId));
						Origin origin = OriginRegistry.get(Identifier.tryParse(originId));
						this.origins.put(layer, origin);
					} catch (IllegalArgumentException e) {
						Switchy.LOGGER.warn("Switchy: Failed to load preset origin with layer" + layerId + " and origin " + originId);
					}
				}
			}
		}
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		SwitchyModuleRegistry.registerModule(ID, OriginsCompat::new, true, SwitchyModuleEditable.ALLOWED);
		OriginsCompatDisplay.touch();
	}
}

package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyPlayer;
import folk.sisby.switchy.SwitchyPresets;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OriginsCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy",  "origins");
	private static final boolean isDefault = true;

	public static final String KEY_ORIGINS_LIST = "OriginLayers";

	// Overwritten on save when null
	@Nullable public Map<OriginLayer, Origin> origins;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		OriginComponent originComponent = ModComponents.ORIGIN.get(player);
		this.origins = new HashMap<>(originComponent.getOrigins());
		SwitchyPresets presets = ((SwitchyPlayer)player).switchy$getPresets();
		if (!presets.getModuleToggles().get(ApoliCompat.ID)) {
			for (OriginLayer layer : this.origins.keySet()) {
				for (PowerType<?> powerType : this.origins.get(layer).getPowerTypes()) {
					Power power = powerType.get(player);
					if (power instanceof InventoryPower inventoryPower) {
						dropInventories(player, inventoryPower);
					}
				}
			}
		}
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		if (this.origins != null) {
			for (OriginLayer layer : this.origins.keySet()) {
				setOrigin(player, layer, this.origins.get(layer));
			}
		}
	}

	private static void setOrigin(PlayerEntity player, OriginLayer layer, Origin origin) {
		OriginComponent component = ModComponents.ORIGIN.get(player);
		component.setOrigin(layer, origin);
		OriginComponent.sync(player);
		boolean hadOriginBefore = component.hadOriginBefore();
		OriginComponent.partialOnChosen(player, hadOriginBefore, origin);
	}

	private static void dropInventories(PlayerEntity player, InventoryPower power) {
		for (int i = 0; i < power.size(); ++i) {
			ItemStack stack = power.getStack(i);
			player.getInventory().offerOrDrop(stack);
		}
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		// From Origins PlayerOriginComponent
		NbtList originLayerList = new NbtList();
		if (this.origins != null) {
			for (Map.Entry<OriginLayer, Origin> entry : origins.entrySet()) {
				NbtCompound layerTag = new NbtCompound();
				layerTag.putString("Layer", entry.getKey().getIdentifier().toString());
				layerTag.putString("Origin", entry.getValue().getIdentifier().toString());
				originLayerList.add(layerTag);
			}
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
					String layerId = layerCompound.getString("Layer");
					String originId = layerCompound.getString("Origin");
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

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, OriginsCompat::new);
	}
}

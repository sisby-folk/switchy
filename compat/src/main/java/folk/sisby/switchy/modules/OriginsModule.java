package folk.sisby.switchy.modules;

import folk.sisby.switchy.SwitchyCompat;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.module.SwitchyModuleTransferable;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
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

	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyModuleRegistry.registerModule(ID, OriginsModule::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.ALLOWED,
				translatable("switchy.modules.switchy.origins.description")
			)
				.withDescriptionWhenEnabled(translatable("switchy.modules.switchy.origins.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.modules.switchy.origins.disabled"))
				.withDeletionWarning(translatable("switchy.modules.switchy.origins.warning"))
		);
	}

	/**
	 * The origins per layer.
	 */
	@Nullable public Map<OriginLayer, Origin> origins;

	private static OriginComponent getForgeComponent(ServerPlayerEntity player) {
		try {
			Class<?> containerClass = Class.forName("io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer");
			Class<?> lazyClass = Class.forName("net.minecraftforge.common.util.LazyOptional");
			Method getMethod = containerClass.getDeclaredMethod("get", Entity.class);
			Method legacyMethod = containerClass.getDeclaredMethod("asLegacyComponent");
			Method elseMethod = lazyClass.getDeclaredMethod("orElse", Object.class);
			Object optionalInstance = getMethod.invoke(null, player);
			Object containerInstance = elseMethod.invoke(optionalInstance, (Object) null);
			return (OriginComponent) legacyMethod.invoke(containerInstance);
		} catch (Exception e) {
			throw new IllegalStateException("Switchy tried and failed to provide origins forge compatibility", e);
		}
	}

	@SuppressWarnings("deprecation")
	private static OriginComponent getComponent(ServerPlayerEntity player) {
		return FabricLoader.getInstance().isModLoaded("connectormod") ? getForgeComponent(player) : ModComponents.ORIGIN.get(player);
	}

	private static void setOrigin(ServerPlayerEntity player, OriginLayer layer, Origin origin) {
		OriginComponent component = getComponent(player);
		component.setOrigin(layer, origin);
		OriginComponent.sync(player);
		boolean hadOriginBefore = component.hadOriginBefore();
		OriginComponent.partialOnChosen(player, hadOriginBefore, origin);
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		OriginComponent component = getComponent(player);
		origins = new HashMap<>(component.getOrigins());
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		if (origins != null) origins.forEach((layer, origin) -> setOrigin(player, layer, origin));
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
						if (layer == null || origin == null) throw new IllegalArgumentException("A layer or origin was null!");
						origins.put(layer, origin);
					} catch (IllegalArgumentException originGetEx) {
						SwitchyCompat.LOGGER.warn("[Switchy Compat] Failed to load preset origin with layer {} and origin {}. Exception: {}", layerId, originId, originGetEx);
					}
				}
			}
		}
	}
}

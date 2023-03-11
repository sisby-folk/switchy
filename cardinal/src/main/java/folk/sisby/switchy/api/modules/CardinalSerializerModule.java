package folk.sisby.switchy.api.modules;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A generic module for switching cardinal entity component data using {@link Component#readFromNbt(NbtCompound)} and {@link Component#writeToNbt(NbtCompound)}.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.8.0
 */
public class CardinalSerializerModule implements SwitchyModule {
	// Generic Fields
	private final Map<Identifier, ComponentConfig<? extends Component>> componentConfigs;
	// Module Data
	private NbtCompound moduleNbt = new NbtCompound();

	private CardinalSerializerModule(Map<Identifier, ComponentConfig<? extends Component>> componentConfigs) {
		this.componentConfigs = componentConfigs;
	}

	/**
	 * A generator for a module instance for a single cardinal component with additional arbitrary "clear" and "sync" callbacks.
	 * Intended for misbehaving cardinal modules that cause data leakage or desync when serialized/deserialized while in use.
	 *
	 * @param registryKey   the key for the cardinal component.
	 * @param preApplyClear operations to perform with the player before applying module data.
	 * @param postApplySync operations to perform with the player after applying module data.
	 * @param <T1>          the component type.
	 * @return a module instance for the specified cardinal component.
	 */
	public static <T1 extends Component> CardinalSerializerModule from(ComponentKey<T1> registryKey, BiConsumer<ComponentKey<T1>, ServerPlayerEntity> preApplyClear, BiConsumer<ComponentKey<T1>, ServerPlayerEntity> postApplySync) {
		return new CardinalSerializerModule(Map.of(registryKey.getId(), new ComponentConfig<>(registryKey, preApplyClear, postApplySync)));
	}

	/**
	 * Register a variant of this type of module using a supplier specific to the specified cardinal component.
	 * Equivalent to data-driven json modules loaded using {@link folk.sisby.switchy.CardinalModuleLoader}.
	 *
	 * @param id              A unique identifier to associate with the module being registered.
	 * @param componentKeyIds a set of cardinal component key IDs to create the module for.
	 * @param moduleInfo      The static settings for the module. See {@link SwitchyModuleInfo}.
	 * @throws IllegalArgumentException when {@code id} is already associated with a registered module.
	 * @throws IllegalStateException    when a {@code uniqueId} provided in {@link SwitchyModuleInfo} collides with one already registered.
	 * @see SwitchyModuleRegistry
	 */
	public static void register(Identifier id, Set<Identifier> componentKeyIds, SwitchyModuleInfo moduleInfo) throws IllegalArgumentException, IllegalStateException  {
		SwitchyModuleRegistry.registerModule(id, () -> {
			Map<Identifier, ComponentConfig<?>> map = new HashMap<>();
			for (Identifier identifier : componentKeyIds) {
				ComponentKey<?> componentKey = ComponentRegistry.get(identifier);
				if (componentKey == null) {
					Switchy.LOGGER.warn("[Switchy] cardinal module {} failed to instantiate, as its component isn't created yet.", id);
					return null;
				}
				map.put(identifier, new ComponentConfig<>(componentKey, (k, p) -> {
				}, (k, p) -> {
				}));
			}
			return new CardinalSerializerModule(map);
		}, moduleInfo.withUniqueIds(componentKeyIds));
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		moduleNbt = new NbtCompound();
		componentConfigs.forEach((id, componentConfig) -> {
			NbtCompound componentCompound = new NbtCompound();
			Component component = componentConfig.registryKey.get(player);
			component.writeToNbt(componentCompound);
			moduleNbt.put(id.toString(), componentCompound);
		});
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		componentConfigs.forEach((id, componentConfig) -> {
			componentConfig.invokePreApplyClear(player);
			componentConfig.registryKey.get(player).readFromNbt(moduleNbt.getCompound(id.toString()));
			componentConfig.invokePostApplySync(player);
			componentConfig.registryKey.sync(player);
		});
	}

	@Override
	public NbtCompound toNbt() {
		return moduleNbt.copy();
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		moduleNbt.copyFrom(nbt);
	}

	private record ComponentConfig<T1 extends Component>(ComponentKey<T1> registryKey,
														 BiConsumer<ComponentKey<T1>, ServerPlayerEntity> preApplyClear,
														 BiConsumer<ComponentKey<T1>, ServerPlayerEntity> postApplySync) {
		void invokePreApplyClear(ServerPlayerEntity player) {
			preApplyClear.accept(registryKey, player);
		}

		void invokePostApplySync(ServerPlayerEntity player) {
			postApplySync.accept(registryKey, player);
		}
	}
}

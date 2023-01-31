package folk.sisby.switchy.api.modules;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class CardinalSerializerCompat implements PresetModule {
	public record ComponentConfig<T1 extends Component>(ComponentKey<T1> registryKey, BiConsumer<ComponentKey<T1>, PlayerEntity> preApplyClear, BiConsumer<ComponentKey<T1>, PlayerEntity> postApplySync) {
		void invokePreApplyClear(PlayerEntity player) {
			preApplyClear.accept(registryKey, player);
		}

		void invokePostApplySync(PlayerEntity player) {
			postApplySync.accept(registryKey, player);
		}
	}

	// Generic Fields
	private final Map<Identifier, ComponentConfig<? extends Component>> componentConfigs;

	// Module Data
	private NbtCompound moduleNbt = new NbtCompound();

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		this.moduleNbt = new NbtCompound();
		componentConfigs.forEach((id, componentConfig) -> {
			NbtCompound componentCompound = new NbtCompound();
			Component component = componentConfig.registryKey.get(player);
			component.writeToNbt(componentCompound);
			this.moduleNbt.put(id.toString(), componentCompound);
		});
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
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
		this.moduleNbt.copyFrom(nbt);
	}

	public CardinalSerializerCompat(Map<Identifier, ComponentConfig<? extends Component>> componentConfigs) {
		this.componentConfigs = componentConfigs;
	}

	public static <T1 extends Component> CardinalSerializerCompat from(ComponentKey<T1> registryKey, BiConsumer<ComponentKey<T1>, PlayerEntity> preApplyClear, BiConsumer<ComponentKey<T1>, PlayerEntity> postApplySync) {
		return new CardinalSerializerCompat(Map.of(registryKey.getId(), new ComponentConfig<>(registryKey, preApplyClear, postApplySync)));
	}

	public static void register(Identifier moduleId, Set<Identifier> componentKeyId, Boolean isDefault, ModuleImportable importable) {
			PresetModuleRegistry.registerModule(moduleId, () -> {
				Map<Identifier, ComponentConfig<?>> map = new HashMap<>();
				for (Identifier identifier : componentKeyId) {
					ComponentKey<?> componentKey = ComponentRegistry.get(identifier);
					if (componentKey == null) {
						Switchy.LOGGER.warn("Switchy: cardinal module {} failed to instantiate, as its component isn't created yet.", moduleId);
						return null;
					}
					map.put(identifier, new ComponentConfig<>(componentKey, (k, p) -> {}, (k, p) -> {}));
				}
				return new CardinalSerializerCompat(map);
			}, isDefault, importable, Set.of(), componentKeyId);
	}
}

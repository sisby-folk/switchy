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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class CardinalSerializerCompat implements PresetModule {
	public record ComponentConfig<T1 extends Component>(ComponentKey<T1> registryKey, BiConsumer<ComponentKey<T1>, PlayerEntity> preApplyClear, BiConsumer<ComponentKey<T1>, PlayerEntity> postApplySync) {}

	// Generic Fields
	private final Map<Identifier, ComponentConfig<? extends Component>> componentConfigs = new HashMap<>();

	// Module Data
	private NbtCompound componentTag = new NbtCompound();

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		this.componentTag = new NbtCompound();
		componentConfigs.forEach((id, componentConfig) -> {
			NbtCompound componentCompound = new NbtCompound();
			Component component = componentConfig.registryKey.get(player);
			component.writeToNbt(componentCompound);
			this.componentTag.put(id.toString(), componentCompound);
		});
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		componentConfigs.forEach((id, componentConfig) -> {
			componentConfig.preApplyClear.accept(componentConfig.registryKey, player);
			componentConfig.registryKey.get(player).readFromNbt(componentTag.getCompound(id.toString()));
			componentConfig.postApplySync.accept(componentConfig.registryKey, player);
			componentConfig.registryKey.sync(player);
		});
	}

	@Override
	public NbtCompound toNbt() {
		return componentTag.copy();
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.componentTag.copyFrom(nbt);
	}

	public CardinalSerializerCompat(Collection<ComponentConfig<? extends Component>> componentConfigs) {
		this.componentConfigs.putAll(componentConfigs.stream().collect(Collectors.toMap(rk -> rk.registryKey().getId(), rk -> rk)));
	}

	public static void register(Identifier moduleId, Set<Identifier> componentKeyId, Boolean isDefault, ModuleImportable importable) {
			PresetModuleRegistry.registerModule(moduleId, () -> {
				Set<ComponentKey<? extends Component>> componentKeys = componentKeyId.stream().map(ComponentRegistry::get).collect(Collectors.toSet());
				if (componentKeys.stream().allMatch(Objects::nonNull)) {
					return new CardinalSerializerCompat(componentKeys.stream().map(registryKey -> new ComponentConfig<>(registryKey, (k, p) -> {}, (k, p) -> {})).collect(Collectors.toSet()));
				} else {
					Switchy.LOGGER.warn("Switchy: cardinal module {} failed to instantiate, as its component isn't created yet.", moduleId);
					return null;
				}
			}, isDefault, importable, Set.of(), componentKeyId);
	}
}

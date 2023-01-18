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
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

public class CardinalSerializerCompat<T1 extends Component> implements PresetModule {
	// Generic Fields
	private final ComponentKey<T1> registryKey;
	private final TriConsumer<ComponentKey<T1>, T1, PlayerEntity> preApplyClear;
	private final TriConsumer<ComponentKey<T1>, T1, PlayerEntity> postApplySync;

	// Module Data
	private NbtCompound componentTag = new NbtCompound();

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		T1 component = registryKey.get(player);
		this.componentTag = new NbtCompound();
		component.writeToNbt(componentTag);
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		T1 component = registryKey.get(player);
		preApplyClear.accept(registryKey, component, player);
		component.readFromNbt(componentTag);
		postApplySync.accept(registryKey, component, player);
		registryKey.sync(player);
	}

	@Override
	public NbtCompound toNbt() {
		return componentTag.copy();
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.componentTag.copyFrom(nbt);
	}

	public CardinalSerializerCompat(ComponentKey<T1> registryKey, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> preApplyClear, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> postApplySync) {
		this.registryKey = registryKey;
		this.preApplyClear = preApplyClear;
		this.postApplySync = postApplySync;
	}

	public CardinalSerializerCompat(ComponentKey<T1> registryKey) {
		this(registryKey, (k, c, p) -> {}, (k, c, p) -> {});
	}

	public static void register(Identifier moduleId, Identifier componentKeyId, Boolean isDefault, ModuleImportable importable) {
			PresetModuleRegistry.registerModule(moduleId, () -> {
				if (ComponentRegistry.get(componentKeyId) != null) {
					return new CardinalSerializerCompat<>(ComponentRegistry.get(componentKeyId));
				} else {
					Switchy.LOGGER.warn("Switchy: cardinal module {} failed to instantiate, as its component isn't created yet.", componentKeyId);
					return null;
				}
			}, isDefault, importable);
	}
}

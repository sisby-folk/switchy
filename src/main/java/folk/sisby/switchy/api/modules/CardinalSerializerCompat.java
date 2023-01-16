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
	private final Identifier ID;
	private final boolean isDefault;
	private final ModuleImportable importable;

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

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	@Override
	public ModuleImportable getImportable() {
		return importable;
	}

	@Deprecated(forRemoval = true)
	public CardinalSerializerCompat(Identifier id, ComponentKey<T1> registryKey, Boolean isDefault, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> preApplyClear, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> postApplySync) {
		this.registryKey = registryKey;
		this.ID = id;
		this.isDefault = isDefault;
		this.importable = ModuleImportable.OPERATOR;
		this.preApplyClear = preApplyClear;
		this.postApplySync = postApplySync;
	}

	public CardinalSerializerCompat(Identifier id, ComponentKey<T1> registryKey, Boolean isDefault, ModuleImportable importable, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> preApplyClear, TriConsumer<ComponentKey<T1>, T1, PlayerEntity> postApplySync) {
		this.registryKey = registryKey;
		this.ID = id;
		this.isDefault = isDefault;
		this.importable = importable;
		this.preApplyClear = preApplyClear;
		this.postApplySync = postApplySync;
	}

	@Deprecated(forRemoval = true)
	public CardinalSerializerCompat(Identifier id, ComponentKey<T1> registryKey, Boolean isDefault) {
		this(id, registryKey, isDefault, (k, c, p) -> {}, (k, c, p) -> {});
	}

	public CardinalSerializerCompat(Identifier id, ComponentKey<T1> registryKey, Boolean isDefault, ModuleImportable importable) {
		this(id, registryKey, isDefault, importable, (k, c, p) -> {}, (k, c, p) -> {});
	}

	public static void tryRegister(Identifier moduleId, Identifier componentKeyId, Boolean isDefault, ModuleImportable importable) {
		ComponentKey<? extends Component> componentKey = ComponentRegistry.get(componentKeyId);
		if (componentKey != null) {
			PresetModuleRegistry.registerModule(moduleId, () -> new CardinalSerializerCompat<>(moduleId, componentKey, isDefault, importable));
		} else {
			Switchy.LOGGER.warn("Switchy: cardinal module {} failed to register, as its component isn't created yet.", componentKeyId);
		}
	}
}

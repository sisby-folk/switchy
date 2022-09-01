package folk.sisby.switchy.api.modules;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import folk.sisby.switchy.api.PresetModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

public class CardinalSerializerCompat<T1 extends Component>  implements PresetModule {
	private final Identifier ID;
	private final boolean isDefault;

	// Generic Fields
	private final ComponentKey<T1> registryKey;
	private final BiConsumer<T1, PlayerEntity> preApplyClear;
	private final BiConsumer<T1, PlayerEntity> postApplySync;

	// Module Data
	private NbtCompound componentTag = new NbtCompound();

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		T1 component = registryKey.get(player);
		this.componentTag = new NbtCompound();
		component.writeToNbt(componentTag);
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		T1 component = registryKey.get(player);
		preApplyClear.accept(component, player);
		component.readFromNbt(componentTag);
		postApplySync.accept(component, player);
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

	public CardinalSerializerCompat(Identifier id, ComponentKey<T1> registryKey, BiConsumer<T1, PlayerEntity> preApplyClear, BiConsumer<T1, PlayerEntity> postApplySync, Boolean isDefault) {
		this.registryKey = registryKey;
		this.ID = id;
		this.preApplyClear = preApplyClear;
		this.postApplySync = postApplySync;
		this.isDefault = isDefault;
	}
}

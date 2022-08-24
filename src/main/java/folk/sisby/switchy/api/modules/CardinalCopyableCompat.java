package folk.sisby.switchy.api.modules;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import folk.sisby.switchy.api.PresetModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class CardinalCopyableCompat<T1 extends CopyableComponent<T1>>  implements PresetModule {
	private final Identifier ID;
	private final boolean isDefault;

	// Generic Fields
	private final ComponentKey<T1> registryKey;

	// Module Data
	private final T1 component;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		this.component.copyFrom(registryKey.get(player));
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		registryKey.get(player).copyFrom(this.component);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		this.component.writeToNbt(outNbt);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.component.readFromNbt(nbt);
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	public CardinalCopyableCompat(Identifier id, ComponentKey<T1> registryKey, Supplier<T1> componentFactory, Boolean isDefault) {
		this.ID = id;
		this.registryKey = registryKey;
		this.component = componentFactory.get();
		this.isDefault = isDefault;
	}
}

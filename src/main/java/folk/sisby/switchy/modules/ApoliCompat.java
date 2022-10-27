package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ApoliCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy", "apoli");
	private static final boolean isDefault = true;
	private static final Collection<Identifier> applyDependencies = Set.of(OriginsCompat.ID);

	public static final String KEY_POWER_DATA_LIST = "PowerData";

	public Map<PowerType<?>, NbtElement> powerNbt;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		this.powerNbt = new HashMap<>();
		List<Power> powers = PowerHolderComponent.KEY.get(player).getPowers();
		for (Power power : powers) {
			this.powerNbt.put(power.getType(), power.toTag());
		}
	}

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		this.updateFromPlayer(player);
		if (nextPreset != null) {
			List<Power> powers = PowerHolderComponent.KEY.get(player).getPowers();
			for (Power power : powers) {
				if (power instanceof InventoryPower) {
					for (int i = 0; i < ((InventoryPower) power).size(); ++i) {
						((InventoryPower) power).removeStack(i);
					}
				}
			}
		}
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		if (this.powerNbt != null) {
			for (Map.Entry<PowerType<?>, NbtElement> entry : powerNbt.entrySet()) {
				Power power = PowerHolderComponent.KEY.get(player).getPower(entry.getKey());
				if (power != null) {
					power.fromTag(entry.getValue());
				}
			}
		}
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		NbtList powerNbtList = new NbtList();
		for (Map.Entry<PowerType<?>, NbtElement> entry : powerNbt.entrySet()) {
			NbtCompound powerTag = new NbtCompound();
			powerTag.putString("PowerType", entry.getKey().getIdentifier().toString());
			powerTag.put("Data", entry.getValue());
			powerNbtList.add(powerTag);
		}
		outNbt.put(KEY_POWER_DATA_LIST, powerNbtList);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.powerNbt = new HashMap<>();
		if (nbt.contains(KEY_POWER_DATA_LIST, NbtElement.LIST_TYPE)) {
			NbtList powerDataList = nbt.getList(KEY_POWER_DATA_LIST, NbtElement.COMPOUND_TYPE);
			for (NbtElement dataElement : powerDataList) {
				if (dataElement instanceof NbtCompound dataCompound) {
					String powerId = dataCompound.getString("PowerType");
					NbtElement powerData = dataCompound.get("Data");
					try {
						PowerType<?> powerType = PowerTypeRegistry.get(Identifier.tryParse(powerId));
						this.powerNbt.put(powerType, powerData);
					} catch (IllegalArgumentException e) {
						Switchy.LOGGER.warn("Switchy: Failed to load preset power with id" + powerId);
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

	@Override
	public Collection<Identifier> getApplyDependencies() {
		return applyDependencies;
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, ApoliCompat::new);
	}
}

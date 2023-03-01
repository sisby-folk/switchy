package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches power data from Apace's Apoli.
 *
 * @author MerchantPug
 * @see SwitchyModule
 * @since 1.5.0
 */
public class ApoliCompat implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "apoli");

	/**
	 * The NBT key where the list of power data is stored.
	 */
	public static final String KEY_POWER_DATA_LIST = "PowerData";

	static {
		SwitchyModuleRegistry.registerModule(ID, ApoliCompat::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.OPERATOR,
				translatable("switchy.compat.module.apoli.description")
		)
				.withDescriptionWhenEnabled(translatable("switchy.compat.module.apoli.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.compat.module.apoli.disabled"))
				.withDeletionWarning(translatable("switchy.compat.module.apoli.warning"))
				.withApplyDependencies(Set.of(OriginsCompat.ID)));
	}

	/**
	 * The NBT data for each power.
	 */
	public final Map<PowerType<?>, NbtElement> powerNbt = new HashMap<>();

	private static void clearInventories(List<InventoryPower> powers) {
		powers.forEach(InventoryPower::clear);
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		powerNbt.clear();
		List<Power> powers = PowerHolderComponent.KEY.get(player).getPowers();
		for (Power power : powers) {
			powerNbt.put(power.getType(), power.toTag());
		}
		if (nextPreset != null) {
			clearInventories(PowerHolderComponent.getPowers(player, InventoryPower.class));
		}
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		powerNbt.forEach((powerType, nbt) -> {
			Power power = PowerHolderComponent.KEY.get(player).getPower(powerType);
			if (power != null) {
				power.fromTag(nbt);
			}
		});
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		NbtList powerNbtList = new NbtList();
		powerNbt.forEach((powerType, nbt) -> {
			NbtCompound powerTag = new NbtCompound();
			powerTag.putString("PowerType", powerType.getIdentifier().toString());
			powerTag.put("Data", nbt);
			powerNbtList.add(powerTag);
		});
		outNbt.put(KEY_POWER_DATA_LIST, powerNbtList);
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		powerNbt.clear();
		if (nbt.contains(KEY_POWER_DATA_LIST, NbtElement.LIST_TYPE)) {
			NbtList powerDataList = nbt.getList(KEY_POWER_DATA_LIST, NbtElement.COMPOUND_TYPE);
			for (NbtElement dataElement : powerDataList) {
				if (dataElement instanceof NbtCompound dataCompound) {
					String powerId = dataCompound.getString("PowerType");
					NbtElement powerData = dataCompound.get("Data");
					try {
						PowerType<?> powerType = PowerTypeRegistry.get(Identifier.tryParse(powerId));
						powerNbt.put(powerType, powerData);
					} catch (IllegalArgumentException e) {
						Switchy.LOGGER.warn("[Switchy] Failed to load preset power with id" + powerId);
					}
				}
			}
		}
	}
}

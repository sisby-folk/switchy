package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.config.ApoliModuleConfig;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.config.QuiltConfig;

import java.util.*;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches power data from Apace's Apoli.
 *
 * @author MerchantPug
 * @see SwitchyModule
 * @since 1.5.0
 */
public class ApoliModule implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "apoli");
	/**
	 * The config object for the apoli module, containing the current state of {@code /config/switchy/apoli.toml}.
	 */
	public static final ApoliModuleConfig CONFIG = QuiltConfig.create(Switchy.ID, "apoli", ApoliModuleConfig.class);
	private static final Identifier COMMAND_SOURCE = new Identifier("apoli", "command");
	/**
	 * The NBT key where the list of serialized apoli:command powers are stored.
	 */
	public static final String KEY_COMMAND_POWERS = "CommandPowers";
	/**
	 * The NBT key where the list of power data is stored.
	 */
	public static final String KEY_POWER_DATA_LIST = "PowerData";

	static {
		SwitchyModuleRegistry.registerModule(ID, ApoliModule::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.OPERATOR,
				translatable("switchy.modules.switchy_compat.apoli.description")
		)
				.withDescriptionWhenEnabled(translatable("switchy.modules.switchy_compat.apoli.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.modules.switchy_compat.apoli.disabled"))
				.withDeletionWarning(translatable("switchy.modules.switchy_compat.apoli.warning"))
				.withApplyDependencies(Set.of(OriginsModule.ID)));
	}

	/**
	 * Powers added by commands allowed to be switched
	 */
	public List<PowerType<?>> commandPowers = null;
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
		PowerHolderComponent playerHolder = PowerHolderComponent.KEY.get(player);
		if (CONFIG.switchCommandPowers || !CONFIG.exceptionPowerIds.isEmpty()) {
			commandPowers = new ArrayList<>();
			for (PowerType<?> powerType : playerHolder.getPowersFromSource(COMMAND_SOURCE)) {
				if (CONFIG.canSwitchPower(powerType) && !commandPowers.contains(powerType)) {
					commandPowers.add(powerType);
				}
			}
		}
		powerNbt.clear();
		for (Power power : playerHolder.getPowers()) {
			powerNbt.put(power.getType(), power.toTag());
		}
		if (nextPreset != null) {
			clearInventories(PowerHolderComponent.getPowers(player, InventoryPower.class));
		}
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		PowerHolderComponent playerHolder = PowerHolderComponent.KEY.get(player);
		if (commandPowers != null) {
			playerHolder.getPowersFromSource(COMMAND_SOURCE).forEach(powerType -> {
				if (CONFIG.canSwitchPower(powerType) && !commandPowers.contains(powerType)) {
					playerHolder.removePower(powerType, COMMAND_SOURCE);
				}
			});
			commandPowers.forEach(powerType -> {
				if (CONFIG.canSwitchPower(powerType) && !playerHolder.hasPower(powerType, COMMAND_SOURCE)) {
					playerHolder.addPower(powerType, COMMAND_SOURCE);
				}
			});
		}
		powerNbt.forEach((powerType, nbt) -> {
			Power power = playerHolder.getPower(powerType);
			if (power != null) {
				power.fromTag(nbt);
			}
		});
	}

	@Override
	public void onDelete(ServerPlayerEntity player, boolean fromDisable) {
		PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(InventoryPower::dropItemsOnLost);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (commandPowers != null) {
			NbtList commandPowerList = new NbtList();
			commandPowers.forEach(powerType -> commandPowerList.add(NbtString.of(powerType.getIdentifier().toString())));
			outNbt.put(KEY_COMMAND_POWERS, commandPowerList);
		}
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
		if (nbt.contains(KEY_COMMAND_POWERS, NbtElement.LIST_TYPE)) {
			NbtList commandPowerList = nbt.getList(KEY_COMMAND_POWERS, NbtElement.STRING_TYPE);
			commandPowers = new ArrayList<>();
			commandPowerList.forEach(id -> {
				try {
					PowerType<?> powerType = PowerTypeRegistry.get(Identifier.tryParse(id.asString()));
					commandPowers.add(powerType);
				} catch (IllegalArgumentException powerGetEx) {
					Switchy.LOGGER.warn("[Switchy] Failed to load preset command power with id {}. Exception: {}", id, powerGetEx);
				}
			});
		}
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
					} catch (IllegalArgumentException powerGetEx) {
						Switchy.LOGGER.warn("[Switchy] Failed to load preset power with id {}. Exception: {}", powerId, powerGetEx);
					}
				}
			}
		}
	}
}

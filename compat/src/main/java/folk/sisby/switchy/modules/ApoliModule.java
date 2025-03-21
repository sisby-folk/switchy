package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyCompat;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.config.ApoliModuleConfig;
import folk.sisby.switchy.util.Feedback;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerManager;
import io.github.apace100.apoli.power.type.InventoryPowerType;
import io.github.apace100.apoli.power.type.PowerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
public class ApoliModule implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = Feedback.identifier("switchy", "apoli");
	/**
	 * The config object for the apoli module, containing the current state of {@code /config/switchy/apoli.toml}.
	 */
	public static final ApoliModuleConfig CONFIG = ApoliModuleConfig.createToml(FabricLoader.getInstance().getConfigDir(), Switchy.ID, "apoli", ApoliModuleConfig.class);
	/**
	 * The NBT key where the list of serialized apoli:command powers are stored.
	 */
	public static final String KEY_COMMAND_POWERS = "CommandPowers";
	/**
	 * The NBT key where the list of power data is stored.
	 */
	public static final String KEY_POWER_DATA_LIST = "PowerData";
	private static final Identifier COMMAND_SOURCE = Feedback.identifier("apoli", "command");
	/**
	 * The NBT data for each power.
	 */
	public final Map<Power, NbtElement> powerNbt = new HashMap<>();
	/**
	 * Powers added by commands allowed to be switched
	 */
	public List<Power> commandPowers = null;

	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyModuleRegistry.registerModule(ID, ApoliModule::new, new SwitchyModuleInfo(
			true,
			SwitchyModuleEditable.OPERATOR,
			translatable("switchy.modules.switchy.apoli.description")
		)
			.withDescriptionWhenEnabled(translatable("switchy.modules.switchy.apoli.enabled"))
			.withDescriptionWhenDisabled(translatable("switchy.modules.switchy.apoli.disabled"))
			.withDeletionWarning(translatable("switchy.modules.switchy.apoli.warning"))
			.withApplyDependencies(Set.of(OriginsModule.ID)));
	}

	private static void clearInventories(List<InventoryPowerType> powers) {
		powers.forEach(InventoryPowerType::clear);
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		PowerHolderComponent playerHolder = PowerHolderComponent.getNullable(player);
		if (CONFIG.switchCommandPowers || !CONFIG.exceptionPowerIds.isEmpty()) {
			commandPowers = new ArrayList<>();
			for (Power power : playerHolder.getPowersFromSource(COMMAND_SOURCE)) {
				if (CONFIG.canSwitchPower(power) && !commandPowers.contains(power)) {
					commandPowers.add(power);
				}
			}
		}
		powerNbt.clear();
		for (PowerType powerType : playerHolder.getPowerTypes()) {
			powerNbt.put(powerType.getPower(), powerType.toTag());
		}
		if (nextPreset != null) {
			clearInventories(PowerHolderComponent.getPowerTypes(player, InventoryPowerType.class));
		}
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		PowerHolderComponent playerHolder = PowerHolderComponent.getNullable(player);
		if (commandPowers != null) {
			playerHolder.getPowersFromSource(COMMAND_SOURCE).forEach(power -> {
				if (CONFIG.canSwitchPower(power) && !commandPowers.contains(power)) {
					playerHolder.removePower(power, COMMAND_SOURCE);
				}
			});
			commandPowers.forEach(power -> {
				if (CONFIG.canSwitchPower(power) && !playerHolder.hasPower(power, COMMAND_SOURCE)) {
					playerHolder.addPower(power, COMMAND_SOURCE);
				}
			});
		}
		powerNbt.forEach((power, nbt) -> {
			PowerType powerType = playerHolder.getPowerType(power);
			if (powerType != null) {
				powerType.fromTag(nbt);
			}
		});
	}

	@Override
	public void onDelete(ServerPlayerEntity player, boolean fromDisable) {
		PowerHolderComponent.getPowerTypes(player, InventoryPowerType.class).forEach(InventoryPowerType::dropItemsOnLost);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (commandPowers != null) {
			NbtList commandPowerList = new NbtList();
			commandPowers.forEach(power -> commandPowerList.add(NbtString.of(power.getId().toString())));
			outNbt.put(KEY_COMMAND_POWERS, commandPowerList);
		}
		NbtList powerNbtList = new NbtList();
		powerNbt.forEach((power, nbt) -> {
			NbtCompound powerTag = new NbtCompound();
			powerTag.putString("PowerType", power.getId().toString());
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
					Power power = PowerManager.get(Identifier.tryParse(id.asString()));
					commandPowers.add(power);
				} catch (IllegalArgumentException powerGetEx) {
					SwitchyCompat.LOGGER.warn("[Switchy Compat] Failed to load preset command power with id {}. Exception: {}", id, powerGetEx);
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
						Power power = PowerManager.get(Identifier.tryParse(powerId));
						powerNbt.put(power, powerData);
					} catch (IllegalArgumentException powerGetEx) {
						SwitchyCompat.LOGGER.warn("[Switchy Compat] Failed to load preset power with id {}. Exception: {}", powerId, powerGetEx);
					}
				}
			}
		}
	}
}

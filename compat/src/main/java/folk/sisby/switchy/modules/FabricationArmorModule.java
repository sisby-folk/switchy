package folk.sisby.switchy.modules;

import com.google.common.base.Enums;
import com.mojang.datafixers.util.Pair;
import com.unascribed.fabrication.features.FeatureHideArmor;
import com.unascribed.fabrication.interfaces.GetSuppressedSlots;
import folk.sisby.switchy.SwitchyCompat;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches hidden armor from unascribed's Fabrication's {@code /hidearmor} feature.
 * "Sticky" - i.e. data will copy from existing presets to new ones, assuming players will prefer the same setup.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.4.0
 */
public class FabricationArmorModule implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "hidearmor");

	/**
	 * The NBT key where the list of EquipmentSlots to hide is stored.
	 */
	public static final String KEY_SUPPRESSED_SLOTS = "suppressedSlots";

	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyModuleRegistry.registerModule(ID, FabricationArmorModule::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.ALLOWED,
				translatable("switchy.modules.switchy.hidearmor.description")
			)
				.withDescriptionWhenEnabled(translatable("switchy.modules.switchy.hidearmor.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.modules.switchy.hidearmor.disabled"))
				.withDeletionWarning(translatable("switchy.modules.switchy.hidearmor.warning"))
				.withApplyDependencies(FabricLoader.getInstance().isModLoaded("fabrictailor") ? Set.of(FabricTailorModule.ID) : Set.of())
		);
	}

	/**
	 * The NBT key where the list of EquipmentSlots to hide is stored.
	 */
	private @Nullable Set<EquipmentSlot> suppressedSlots;

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		if (player instanceof GetSuppressedSlots gss) {
			suppressedSlots = new HashSet<>();
			suppressedSlots.addAll(gss.fabrication$getSuppressedSlots());
		}
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		if (suppressedSlots != null && player instanceof GetSuppressedSlots gss) {
			gss.fabrication$getSuppressedSlots().clear();
			gss.fabrication$getSuppressedSlots().addAll(suppressedSlots);

			// Sketchily copied from feature
			((ServerWorld) player.world).getChunkManager().sendToOtherNearbyPlayers(player, new EntityEquipmentUpdateS2CPacket(player.getId(), Arrays.stream(EquipmentSlot.values()).map((es) ->
				Pair.of(es, gss.fabrication$getSuppressedSlots().contains(es) ? ItemStack.EMPTY : player.getEquippedStack(es))).toList()));
			FeatureHideArmor.sendSuppressedSlotsForSelf(player);
		}
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (suppressedSlots != null) {
			NbtList slotList = new NbtList();
			for (EquipmentSlot slot : suppressedSlots) {
				slotList.add(NbtString.of(slot.name().toLowerCase(Locale.ROOT)));
			}
			outNbt.put(KEY_SUPPRESSED_SLOTS, slotList);
		}
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		suppressedSlots = EnumSet.noneOf(EquipmentSlot.class);
		NbtList nbtList = nbt.getList(KEY_SUPPRESSED_SLOTS, NbtElement.STRING_TYPE);
		for (int i = 0; i < nbtList.size(); i++) {
			EquipmentSlot slot = Enums.getIfPresent(EquipmentSlot.class, nbtList.getString(i).toUpperCase(Locale.ROOT)).orNull();
			if (slot == null) {
				SwitchyCompat.LOGGER.warn("[Switchy Compat] Unrecognized slot {} while loading profile", nbtList.getString(i));
			} else {
				suppressedSlots.add(slot);
			}
		}
	}
}

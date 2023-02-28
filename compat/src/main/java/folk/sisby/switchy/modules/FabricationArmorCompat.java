package folk.sisby.switchy.modules;

import com.google.common.base.Enums;
import com.mojang.datafixers.util.Pair;
import com.unascribed.fabrication.features.FeatureHideArmor;
import com.unascribed.fabrication.interfaces.GetSuppressedSlots;
import folk.sisby.switchy.Switchy;
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
import org.quiltmc.loader.api.QuiltLoader;

import java.util.*;

/**
 * A module that switches whether armor is hidden using unascribed's Fabrication's {@code /hidearmor} feature.
 * "Sticky" - i.e. data will copy from existing presets to new ones, assuming players will prefer the same setup.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.4.0
 */
public class FabricationArmorCompat implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "fabrication_hidearmor");

	/**
	 * The NBT key where the list of EquipmentSlots to hide is stored.
	 */
	public static final String KEY_SUPPRESSED_SLOTS = "suppressedSlots";

	static {
		SwitchyModuleRegistry.registerModule(ID, FabricationArmorCompat::new, new SwitchyModuleInfo(true, SwitchyModuleEditable.ALLOWED, QuiltLoader.isModLoaded("fabrictailor") ? Set.of(FabricTailorCompat.ID) : Set.of()));
	}

	/**
	 * The NBT key where the list of EquipmentSlots to hide is stored.
	 */
	private @Nullable Set<EquipmentSlot> suppressedSlots;

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

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
				Switchy.LOGGER.warn("[Switchy] Unrecognized slot " + nbtList.getString(i) + " while loading profile");
			} else {
				suppressedSlots.add(slot);
			}
		}
	}
}

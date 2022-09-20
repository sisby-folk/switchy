package folk.sisby.switchy.modules;

import com.google.common.base.Enums;
import com.mojang.datafixers.util.Pair;
import com.unascribed.fabrication.features.FeatureHideArmor;
import com.unascribed.fabrication.interfaces.GetSuppressedSlots;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FabricationArmorCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy", "fabrication_hidearmor");
	private static final boolean isDefault = true;

	public static final String KEY_SUPPRESSED_SLOTS = "suppressedSlots";

	// Overwritten on save when null
	private @Nullable Set<EquipmentSlot> suppressedSlots;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		this.suppressedSlots = new HashSet<>();
		this.suppressedSlots.addAll(((GetSuppressedSlots) player).fabrication$getSuppressedSlots());
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		if (this.suppressedSlots != null) {
			Set<EquipmentSlot> playerSuppressed = ((GetSuppressedSlots) player).fabrication$getSuppressedSlots();
			playerSuppressed.clear();
			playerSuppressed.addAll(suppressedSlots);

			// Sketchily copied from feature
			((ServerWorld) player.world).getChunkManager().sendToOtherNearbyPlayers(player, new EntityEquipmentUpdateS2CPacket(player.getId(), Arrays.stream(EquipmentSlot.values()).map((es) ->
					Pair.of(es, playerSuppressed.contains(es) ? ItemStack.EMPTY : player.getEquippedStack(es))).toList()));
			FeatureHideArmor.sendSuppressedSlotsForSelf((ServerPlayerEntity) player);
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
		NbtList nbtList = nbt.getList(KEY_SUPPRESSED_SLOTS, NbtType.STRING);
		for (int i = 0; i < nbtList.size(); i++) {
			EquipmentSlot slot = Enums.getIfPresent(EquipmentSlot.class, nbtList.getString(i).toUpperCase(Locale.ROOT)).orNull();
			if (slot == null) {
				Switchy.LOGGER.warn("Switchy: Unrecognized slot " + nbtList.getString(i) + " while loading profile");
			} else {
				suppressedSlots.add(slot);
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
		return List.of(FabricTailorCompat.ID);
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, FabricationArmorCompat::new);
	}
}

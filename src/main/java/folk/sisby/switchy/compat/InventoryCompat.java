package folk.sisby.switchy.compat;

import folk.sisby.switchy.api.PresetCompatModule;
import folk.sisby.switchy.api.SwitchyModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

public class InventoryCompat extends PresetCompatModule {
	private static final Identifier ID = new Identifier("switchy",  "inventories");
	private static final boolean isDefault = false;

	public static final String KEY_INVENTORY_LIST = "inventory";

	private final PlayerInventory inventory = new PlayerInventory(null);

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		this.inventory.clone(player.getInventory());
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		player.getInventory().clone(this.inventory);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		outNbt.put(KEY_INVENTORY_LIST, inventory.writeNbt(new NbtList()));
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		this.inventory.readNbt(nbt.getList(KEY_INVENTORY_LIST, NbtElement.COMPOUND_TYPE));
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	public static void touch() {
	}

	// Runs on touch() - but only once.
	static {
		SwitchyModuleRegistry.registerModule(ID, InventoryCompat::new);
	}
}

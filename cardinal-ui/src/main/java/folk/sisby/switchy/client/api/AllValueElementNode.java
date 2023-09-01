package folk.sisby.switchy.client.api;

import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.List;
import java.util.function.Supplier;

public class AllValueElementNode implements NbtPathArgumentType.PathNode {
	public static final AllValueElementNode INSTANCE = new AllValueElementNode();

	private AllValueElementNode() {
	}

	@Override
	public void get(NbtElement nbt, List<NbtElement> results) {
		if (nbt instanceof NbtCompound compound) {
			compound.getKeys().stream().map(compound::get).forEach(results::add);
		}
	}

	@Override
	public void getOrInit(NbtElement nbt, Supplier<NbtElement> source, List<NbtElement> results) {
		if (nbt instanceof NbtCompound compound) {
			if (compound.isEmpty()) {
				results.add(source.get());
			} else {
				get(nbt, results);
			}
		}
	}

	@Override
	public NbtElement init() {
		return new NbtCompound();
	}

	@Override
	public int set(NbtElement nbt, Supplier<NbtElement> source) {
		if (nbt instanceof NbtCompound compound) {
			if (compound.isEmpty()) {
				compound.put("default", source.get());
				return 1;
			} else {
				NbtElement nbtElement = source.get();
				int unequal_count = (int) compound.getKeys().stream().map(compound::get).filter(nbtElement::equals).count();
				if (unequal_count > 0) {
					compound.getKeys().forEach(k -> compound.put(k, source.get()));
				}
				return unequal_count;
			}
		} else {
			return 0;
		}
	}

	@Override
	public int clear(NbtElement nbt) {
		if (nbt instanceof NbtCompound compound) {
			int i = compound.getKeys().size();
			if (i > 0) {
				compound.getKeys().forEach(compound::remove);
				return i;
			}
		}
		return 0;
	}
}

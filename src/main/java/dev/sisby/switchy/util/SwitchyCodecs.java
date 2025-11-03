package dev.sisby.switchy.util;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface SwitchyCodecs {
	Codec<Set<Identifier>> IDENTIFIER_SET_CODEC = Codec.list(Identifier.CODEC).xmap(LinkedHashSet::new, ArrayList::new);
	Codec<NbtElement> NBT = Codec.PASSTHROUGH.comapFlatMap(dynamic -> DataResult.success(dynamic.convert(NbtOps.INSTANCE).getValue()), nbt -> new Dynamic<>(NbtOps.INSTANCE, nbt));
	MapCodec<ItemStack> ITEM_STACK_MAP_CODEC = new RecursiveMapCodec<>(
		codec -> RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Registries.ITEM.getCodec().fieldOf("id").forGetter(ItemStack::getItem),
					Codec.INT.fieldOf("Count").forGetter(ItemStack::getCount),
					NbtCompound.CODEC.optionalFieldOf("tag").forGetter(stack -> Optional.ofNullable(stack.getNbt()))
				)
				.apply(instance, (id, count, tag) -> {
					ItemStack newStack = new ItemStack(id, count);
					newStack.setNbt(tag.orElse(null));
					return newStack;
				})
		)
	);

	class RecursiveMapCodec<A> extends MapCodec<A> {
		private final Supplier<MapCodec<A>> wrapped;

		private RecursiveMapCodec(final Function<Codec<A>, MapCodec<A>> wrapped) {
			this.wrapped = Suppliers.memoize(() -> wrapped.apply(codec()));
		}

		@Override
		public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
			return wrapped.get().encode(input, ops, prefix);
		}

		@Override
		public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
			return wrapped.get().decode(ops, input);
		}

		@Override
		public <T> Stream<T> keys(final DynamicOps<T> ops) {
			return wrapped.get().keys(ops);
		}
	}

	Codec<DefaultedList<ItemStack>> INVENTORY_CODEC = Codec.list(StackWithSlot.CODEC).xmap(l -> {
		DefaultedList<ItemStack> dl = DefaultedList.ofSize(l.stream().mapToInt(s -> s.slot + 1).max().orElse(0), ItemStack.EMPTY);
		l.forEach(sws -> dl.set(sws.slot(), sws.stack()));
		return dl;
	}, dl -> {
		List<StackWithSlot> l = new ArrayList<>();
		for (int i = 0; i < dl.size(); i++) {
			if (!dl.get(i).isEmpty()) {
				l.add(new StackWithSlot(i, dl.get(i)));
			}
		}
		return l;
	});

	record StackWithSlot(int slot, ItemStack stack) {
		public static final Codec<StackWithSlot> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.intRange(0, 255).fieldOf("Slot").orElse(0).forGetter(StackWithSlot::slot),
				ITEM_STACK_MAP_CODEC.forGetter(StackWithSlot::stack)
			).apply(instance, StackWithSlot::new)
		);
	}
}

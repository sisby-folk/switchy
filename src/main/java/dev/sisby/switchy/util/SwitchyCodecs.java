package dev.sisby.switchy.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface SwitchyCodecs {
	PrimitiveCodec<Byte> BYTE = new PrimitiveCodec<>() {
		@Override
		public <T> DataResult<Byte> read(final DynamicOps<T> ops, final T input) {
			return ops.getNumberValue(input).map(Number::byteValue);
		}

		@Override
		public <T> T write(final DynamicOps<T> ops, final Byte value) {
			return ops.createByte(value);
		}
	};
	Codec<Tag> NBT = Codec.PASSTHROUGH.comapFlatMap(dynamic -> DataResult.success(dynamic.convert(NbtOps.INSTANCE).getValue()), nbt -> new Dynamic<>(NbtOps.INSTANCE, nbt));
	MapCodec<ItemStack> ITEM_STACK_MAP_CODEC = MapCodec.recursive(
		"ItemStack",
		codec -> RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::typeHolder),
					ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
					DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
				)
				.apply(instance, ItemStack::new)
		)
	);

	Codec<NonNullList<ItemStack>> INVENTORY_CODEC = Codec.list(StackWithSlot.CODEC).xmap(l -> {
		NonNullList<ItemStack> dl = NonNullList.withSize(l.stream().mapToInt(s -> s.slot + 1).max().orElse(0), ItemStack.EMPTY);
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

	Codec<Map<String, ItemStack>> EQUIPMENT_CODEC = Codec.unboundedMap(
		Codec.STRING,
		ITEM_STACK_MAP_CODEC.codec()
	);

	record StackWithSlot(int slot, ItemStack stack) {
		public static final Codec<StackWithSlot> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				BYTE.fieldOf("Slot").xmap(b -> b & 0xFF, Integer::byteValue).forGetter(StackWithSlot::slot),
				ITEM_STACK_MAP_CODEC.forGetter(StackWithSlot::stack)
			).apply(instance, StackWithSlot::new)
		);
	}
}

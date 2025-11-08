package dev.sisby.switchy.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface SwitchyCodecs {
	Codec<Set<Identifier>> IDENTIFIER_SET_CODEC = Codec.list(Identifier.CODEC).xmap(LinkedHashSet::new, ArrayList::new);
	Codec<NbtElement> NBT = Codec.PASSTHROUGH.comapFlatMap(dynamic -> DataResult.success(dynamic.convert(NbtOps.INSTANCE).getValue()), nbt -> new Dynamic<>(NbtOps.INSTANCE, nbt));
	MapCodec<ItemStack> ITEM_STACK_MAP_CODEC = MapCodec.recursive(
		"ItemStack",
		codec -> RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Registries.ITEM.getEntryCodec().fieldOf("id").forGetter(ItemStack::getRegistryEntry),
					Codecs.rangedInt(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
					ComponentChanges.CODEC.optionalFieldOf("components", ComponentChanges.EMPTY).forGetter(ItemStack::getComponentChanges)
				)
				.apply(instance, ItemStack::new)
		)
	);

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

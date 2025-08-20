package dev.sisby.switchy.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface SwitchyCodecs {
	Codec<Set<Identifier>> IDENTIFIER_SET_CODEC = Codec.list(Identifier.CODEC).xmap(LinkedHashSet::new, ArrayList::new);
	Codec<Set<SwitchyComponentType<?>>> COMPONENT_TYPE_SET_CODEC = Codec.list(SwitchyComponentTypes.instance().codec()).xmap(LinkedHashSet::new, ArrayList::new);
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
				Codecs.UNSIGNED_BYTE.fieldOf("Slot").orElse(0).forGetter(StackWithSlot::slot),
				ITEM_STACK_MAP_CODEC.forGetter(StackWithSlot::stack)
			).apply(instance, StackWithSlot::new)
		);
	}

	static <B extends ByteBuf, K, V, M extends Map<K, V>> PacketCodec<B, M> packetDispatchedMap(
		IntFunction<? extends M> factory, PacketCodec<? super B, K> keyCodec, Function<K, PacketCodec<? super B, V>> valueCodec
	) {
		return new PacketCodec<>() {
			public void encode(B byteBuf, M map) {
				PacketCodecs.writeCollectionSize(byteBuf, map.size(), Integer.MAX_VALUE);
				map.forEach((k, v) -> {
					keyCodec.encode(byteBuf, k);
					valueCodec.apply(k).encode(byteBuf, v);
				});
			}

			public M decode(B byteBuf) {
				int i = PacketCodecs.readCollectionSize(byteBuf, Integer.MAX_VALUE);
				M map = factory.apply(Math.min(i, 65536));

				for (int j = 0; j < i; j++) {
					K object = keyCodec.decode(byteBuf);
					V object2 = valueCodec.apply(object).decode(byteBuf);
					map.put(object, object2);
				}

				return map;
			}
		};
	}
}

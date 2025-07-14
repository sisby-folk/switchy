package dev.sisby.switchy.util;

import com.mojang.serialization.Codec;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface SwitchyCodecs {
	Codec<Set<Identifier>> IDENTIFIER_SET_CODEC = Codec.list(Identifier.CODEC).xmap(LinkedHashSet::new, ArrayList::new);
	Codec<Set<SwitchyComponentType<?>>> COMPONENT_TYPE_SET_CODEC = Codec.list(SwitchyComponentTypes.instance().codec()).xmap(LinkedHashSet::new, ArrayList::new);

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

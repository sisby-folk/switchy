package dev.sisby.switchy.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public abstract class TypeRegistry<T extends TypeRegistry.Type> {
	private final Codec<T> codec = Identifier.CODEC.comapFlatMap(id -> Optional.ofNullable(get(id))
			.map(DataResult::success)
			.orElse(DataResult.error(() -> "No type found with id " + id)), this::id);
	private final BiMap<Identifier, T> map = HashBiMap.create();

	public Codec<T> codec() {
		return codec;
	}

	public boolean contains(Identifier id) {
		return map.containsKey(id);
	}

	public boolean contains(T id) {
		return map.containsValue(id);
	}

	public T get(Identifier id) {
		return map.get(id);
	}

	public Identifier id(T type) {
		return map.inverse().get(type);
	}

	public Set<Identifier> keys() {
		return Set.copyOf(map.keySet());
	}

	public Set<T> values() {
		return Set.copyOf(map.values());
	}

	public <B extends T> B register(Identifier id, Function<Identifier,B> typeSupplier) {
		B type = typeSupplier.apply(id);
		if (contains(id) || contains(type)) {
			throw new IllegalArgumentException("Type double-registration with ID: %s".formatted(id));
		}
		map.put(id, type);
		return type;
	}

	public interface Type {
		Identifier id();
	}
}

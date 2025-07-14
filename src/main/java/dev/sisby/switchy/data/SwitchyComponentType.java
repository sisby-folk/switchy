package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface SwitchyComponentType<T> extends TypeRegistry.Type {
	Codec<Map<SwitchyComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = Codec.dispatchedMap(SwitchyComponentTypes.instance().codec(), SwitchyComponentType::codec);

	static <T> SwitchyComponentType.Builder<T> builder(@NotNull Identifier id) {
		return new SwitchyComponentType.Builder<>(id);
	}

	@Nullable
	Codec<T> codec();

	SwitchyLayerType defaultLayerType();

	Set<SwitchyLayerType> allowedLayerTypes();

	@Nullable
	PacketCodec<? super RegistryByteBuf, T> packetCodec();

	class Builder<T> {
		@NotNull
		private final Identifier id;
		@Nullable
		private Codec<T> codec;
		@Nullable
		private PacketCodec<? super RegistryByteBuf, T> packetCodec;
		@Nullable
		private SwitchyLayerType defaultLayerType;
		@Nullable
		private Set<SwitchyLayerType> allowedLayerTypes;

		public Builder(@NotNull Identifier id) {
			this.id = id;
		}

		public SwitchyComponentType.Builder<T> codec(Codec<T> codec) {
			this.codec = codec;
			return this;
		}

		public SwitchyComponentType.Builder<T> packetCodec(PacketCodec<? super RegistryByteBuf, T> packetCodec) {
			this.packetCodec = packetCodec;
			return this;
		}

		public Builder<T> defaultLayerType(@Nullable SwitchyLayerType defaultLayerType) {
			this.defaultLayerType = defaultLayerType;
			return this;
		}

		public Builder<T> allowedLayerTypes(@Nullable Set<SwitchyLayerType> allowedLayerTypes) {
			this.allowedLayerTypes = allowedLayerTypes;
			return this;
		}

		public SwitchyComponentType<T> build() {
			return new SimpleSwitchyComponentType<>(
				this.id,
				this.codec,
				this.packetCodec,
				Objects.requireNonNullElse(defaultLayerType, SwitchyLayerTypes.PRIMARY),
				Objects.requireNonNullElse(allowedLayerTypes, Set.of(SwitchyLayerTypes.PRIMARY))
			);
		}

		record SimpleSwitchyComponentType<T>(
			Identifier id,
			@Nullable Codec<T> codec,
			@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec,
			SwitchyLayerType defaultLayerType,
			Set<SwitchyLayerType> allowedLayerTypes
		) implements SwitchyComponentType<T> {
			@Override
			public String toString() {
				return id.toString();
			}
		}
	}
}

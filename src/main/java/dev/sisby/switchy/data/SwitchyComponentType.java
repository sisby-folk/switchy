package dev.sisby.switchy.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface SwitchyComponentType<T> extends TypeRegistry.Type {
	Codec<Map<SwitchyComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = Codec.dispatchedMap(SwitchyComponentTypes.instance().codec(), SwitchyComponentType::codec);

	static <T> SwitchyComponentType.Builder<T> builder(@NotNull Identifier id, @NotNull Codec<T> codec) {
		return new SwitchyComponentType.Builder<>(id, codec);
	}

	@Nullable Codec<T> codec();

	@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec();

	@Nullable Initializer<T> initializer();

	default void tryInitialize(SwitchyComponentMap.Builder builder, Initializer.InitializerContext context) throws Exception {
		if (initializer() == null) return;
		T value = initializer().initialize(context);
		if (value == null) return;
		builder.add(this, value);
	}

	@Nullable Reader<T> reader();

	@Nullable NbtMutator<T> nbtMutator();

	default void tryMutate(SwitchyComponentMap components, NbtCompound playerData) throws Exception {
		if (nbtMutator() != null) {
			nbtMutator().mutate(new NbtMutator.NbtMutatorContext<>(components.get(this), playerData));
		}
	}

	@Nullable LiveMutator<T> liveMutator();

	@Nullable EmptyChecker<T> emptyChecker();

	@FunctionalInterface
	interface Initializer<T> {
		T initialize(InitializerContext context) throws Exception;
		record InitializerContext(NbtCompound playerData, ServerPlayerEntity player) {}
	}

	@FunctionalInterface
	interface Reader<T> {
		T read(ReaderContext context) throws Exception;
		record ReaderContext(NbtCompound playerData, ServerPlayerEntity oldPlayer) {}
	}

	@FunctionalInterface
	interface NbtMutator<T> {
		void mutate(NbtMutatorContext<T> context) throws Exception;
		record NbtMutatorContext<T>(T componentData, NbtCompound playerData) {}
	}

	@FunctionalInterface
	interface LiveMutator<T> {
		void mutate(LiveMutatorContext<T> context) throws Exception;
		record LiveMutatorContext<T>(T componentData, ServerPlayerEntity newPlayer) {}
	}

	@FunctionalInterface
	interface EmptyChecker<T> {
		boolean isEmpty(EmptyCheckerContext<T> context);
		record EmptyCheckerContext<T>(T componentData, ServerPlayerEntity player) {}
	}

	class DefaultPlayerInitializer<T> implements Initializer<T> {
		private final Reader<T> reader;

		public DefaultPlayerInitializer(Reader<T> reader) {
			this.reader = reader;
		}

		@Override
		public T initialize(InitializerContext context) throws Exception {
			ServerPlayerEntity defaultPlayer = context.player().getServer().getPlayerManager().createPlayer(context.player().getGameProfile(), context.player().getClientOptions());
			NbtCompound defaultNbt = new NbtCompound();
			defaultPlayer.writeNbt(defaultNbt);
			return reader.read(new Reader.ReaderContext(defaultNbt, defaultPlayer));
		}
	}

	class NbtSwitcher<T> implements NbtMutator<T>, Reader<T> {
		private final NbtPathArgumentType.NbtPath nbtPath;
		private final Codec<T> codec;

		public NbtSwitcher(NbtPathArgumentType.NbtPath nbtPath, Codec<T> codec) {
			this.nbtPath = nbtPath;
			this.codec = codec;
		}

		@Override
		public T read(ReaderContext context) throws Exception {
			return codec.parse(NbtOps.INSTANCE, nbtPath.get(context.playerData()).getFirst()).getOrThrow();
		}

		@Override
		public void mutate(NbtMutatorContext<T> context) throws Exception {
			nbtPath.put(context.playerData(), codec.encodeStart(NbtOps.INSTANCE, context.componentData()).getOrThrow());
		}

		@Override
		public String toString() {
			return nbtPath.toString();
		}
	}

	record SimpleSwitchyComponentType<T>(
		Identifier id,
		@Nullable Codec<T> codec,
		@Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec,
		@Nullable Initializer<T> initializer,
		@Nullable Reader<T> reader,
		@Nullable NbtMutator<T> nbtMutator,
		@Nullable LiveMutator<T> liveMutator,
		@Nullable EmptyChecker<T> emptyChecker
	) implements SwitchyComponentType<T> {
		@Override
		public String toString() {
			return id.toString();
		}
	}

	class Builder<T> {
		private final @NotNull Identifier id;
		private final @NotNull Codec<T> codec;
		private @Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec;
		private @Nullable Initializer<T> initializer;
		private @Nullable Reader<T> reader;
		private @Nullable NbtMutator<T> nbtMutator;
		private @Nullable LiveMutator<T> liveMutator;
		private @Nullable EmptyChecker<T> emptyChecker;

		public Builder(@NotNull Identifier id, @NotNull Codec<T> codec) {
			this.id = id;
			this.codec = codec;
		}

		public Builder<T> packetCodec(PacketCodec<? super RegistryByteBuf, T> packetCodec) {
			this.packetCodec = packetCodec;
			return this;
		}

		public Builder<T> initializer(@Nullable Initializer<T> initializer) {
			this.initializer = initializer;
			return this;
		}

		public Builder<T> reader(@Nullable Reader<T> reader) {
			this.reader = reader;
			return this;
		}

		public Builder<T> nbtMutator(@Nullable NbtMutator<T> nbtMutator) {
			this.nbtMutator = nbtMutator;
			return this;
		}

		public Builder<T> liveMutator(@Nullable LiveMutator<T> liveMutator) {
			this.liveMutator = liveMutator;
			return this;
		}

		public Builder<T> emptyChecker(@Nullable EmptyChecker<T> emptyChecker) {
			this.emptyChecker = emptyChecker;
			return this;
		}

		public Builder<T> nbtSwitcher(String nbtPath) {
			NbtSwitcher<T> switcher;
			try {
				switcher = new NbtSwitcher<>(NbtPathArgumentType.NbtPath.parse(nbtPath), codec);
			} catch (CommandSyntaxException e) {
				throw new RuntimeException(e);
			}
			this.reader = switcher;
			this.nbtMutator = switcher;
			this.initializer = new DefaultPlayerInitializer<>(switcher);
			return this;
		}

		public SwitchyComponentType<T> build() {
			return new SwitchyComponentType.SimpleSwitchyComponentType<>(
				this.id,
				this.codec,
				this.packetCodec,
				this.initializer,
				this.reader,
				this.nbtMutator,
				this.liveMutator,
				this.emptyChecker
			);
		}
	}
}

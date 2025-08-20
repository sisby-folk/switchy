package dev.sisby.switchy.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.exception.ComponentFailedInitializeException;
import dev.sisby.switchy.exception.KeyNotFoundException;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface SwitchyComponentType<T> extends TypeRegistry.Type {
	Codec<Map<SwitchyComponentType<?>, Object>> TYPE_TO_VALUE_MAP_CODEC = Codec.dispatchedMap(SwitchyComponentTypes.instance().codec(), SwitchyComponentType::codec);

	static <T> SwitchyComponentType.Builder<T> builder(@NotNull Identifier id, @NotNull Codec<T> codec) {
		return new SwitchyComponentType.Builder<>(id, codec);
	}

	@NotNull Codec<T> codec();

	@Nullable Initializer<T> initializer();

	@Nullable SwitchyComponentType.NbtReader<T> nbtReader();

	@Nullable NbtMutator<T> nbtMutator();

	@Nullable EmptyChecker<T> emptyChecker();

	@Nullable TextProvider<T> textProvider();

	@Nullable ArgumentEditor<T> argumentEditor();

	default @Nullable PacketCodec<? super RegistryByteBuf, T> packetCodec() {
		return PacketCodecs.codec(codec());
	}

	default void tryInitialize(Collection<SwitchyComponentMap> consumer, NbtCompound nbt, ServerPlayerEntity player) {
		Initializer<T> initializer = initializer();
		if (initializer == null) return;
		T value = initializer.initialize(nbt, player);
		if (value == null) return;
		consumer.forEach(c -> c.set(this, value));
	}

	default void tryMutate(SwitchyComponentMap components, NbtCompound playerData) {
		NbtMutator<T> mutator = nbtMutator();
		if (mutator != null) {
			mutator.mutate(components.get(this), playerData);
		}
	}

	default MutableText asText(T value) {
		TextProvider<T> textProvider = textProvider();
		if (textProvider != null) {
			return textProvider.toText(value).copy();
		} else {
			return Text.literal(value.toString());
		}
	}

	default void tryCreateEditor(Consumer<ArgumentBuilder<ServerCommandSource, ?>> consumer) {
		ArgumentEditor<T> editor = argumentEditor();
		if (editor != null) {
			consumer.accept(editor.create((c, v) -> SwitchyCommands.execute(c, (i, p, d, f) -> SwitchyCommands.editComponent(p, d, f, c.getArgument("profile", String.class).toLowerCase(), this, v))));
		}
	}

	default MutableText asText(SwitchyComponentMap components) {
		return asText(components.get(this));
	}

	default boolean isPrecious(SwitchyComponentMap components) {
		EmptyChecker<T> emptyChecker = emptyChecker();
		if (emptyChecker != null) {
			return !emptyChecker.isEmpty(components.get(this));
		}
		return false;
	}

	@FunctionalInterface
	interface Initializer<T> {
		T initialize(NbtCompound playerNbt, ServerPlayerEntity player) throws ComponentFailedInitializeException;
	}

	@FunctionalInterface
	interface NbtReader<T> {
		T read(NbtCompound nbt) throws NbtException;
	}

	@FunctionalInterface
	interface NbtMutator<T> {
		void mutate(T value, NbtCompound nbt) throws NbtException;
	}

	@FunctionalInterface
	interface EmptyChecker<T> {
		boolean isEmpty(T value);
	}

	@FunctionalInterface
	interface TextProvider<T> {
		Text toText(T value);
	}

	@FunctionalInterface
	interface ArgumentEditor<T> {
		ArgumentBuilder<ServerCommandSource, ?> create(EditExecutor<T> executor);
	}

	@FunctionalInterface
	interface EditExecutor<T> {
		int execute(CommandContext<ServerCommandSource> context, T value);
	}

	record SimpleTextProvider<T>(Function<T, Text> provider) implements TextProvider<T> {
		@Override
		public Text toText(T value) {
			return provider.apply(value);
		}
	}

	record SimpleEmptyChecker<T>(Predicate<T> predicate) implements EmptyChecker<T> {
		@Override
		public boolean isEmpty(T value) {
			return predicate.test(value);
		}
	}

	record SimpleArgumentEditor<T>(Function<EditExecutor<T>, ArgumentBuilder<ServerCommandSource, ?>> editor) implements ArgumentEditor<T> {
		@Override
		public ArgumentBuilder<ServerCommandSource, ?> create(EditExecutor<T> executor) {
			return editor.apply(executor);
		}
	}

	class DefaultPlayerInitializer<T> implements Initializer<T> {
		private final NbtReader<T> nbtReader;

		public DefaultPlayerInitializer(NbtReader<T> nbtReader) {
			this.nbtReader = nbtReader;
		}

		@Override
		public T initialize(NbtCompound playerNbt, ServerPlayerEntity player) throws ComponentFailedInitializeException {
			ServerPlayerEntity defaultPlayer = player.getServer().getPlayerManager().createPlayer(player.getGameProfile(), player.getClientOptions());
			NbtCompound defaultNbt = new NbtCompound();
			defaultPlayer.writeNbt(defaultNbt);
			try {
				return nbtReader.read(defaultNbt);
			} catch (Exception ignored) {
				try {
					return nbtReader.read(playerNbt);
				} catch (Exception e) {
					throw new ComponentFailedInitializeException("", e);
				}
			}
		}
	}

	class NbtSwitcher<T> implements NbtMutator<T>, NbtReader<T> {
		private final NbtPathArgumentType.NbtPath nbtPath;
		private final Codec<T> codec;

		public NbtSwitcher(NbtPathArgumentType.NbtPath nbtPath, Codec<T> codec) {
			this.nbtPath = nbtPath;
			this.codec = codec;
		}

		@Override
		public T read(NbtCompound nbt) {
			try {
				return codec.parse(NbtOps.INSTANCE, nbtPath.get(nbt).getFirst()).getOrThrow();
			} catch (CommandSyntaxException e) {
				throw new KeyNotFoundException("NBT path returned no valid nodes!");
			}
		}

		@Override
		public void mutate(T value, NbtCompound nbt) throws NbtException {
			try {
				nbtPath.put(nbt, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
			} catch (CommandSyntaxException e) {
				throw new NbtException("NBT path too deep!");
			}
		}

		@Override
		public String toString() {
			return nbtPath.toString();
		}
	}

	record SimpleSwitchyComponentType<T>(
		Identifier id,
		@Nullable Codec<T> codec,
		@Nullable Initializer<T> initializer,
		@Nullable SwitchyComponentType.NbtReader<T> nbtReader,
		@Nullable NbtMutator<T> nbtMutator,
		@Nullable EmptyChecker<T> emptyChecker,
		@Nullable TextProvider<T> textProvider,
		@Nullable ArgumentEditor<T> argumentEditor
	) implements SwitchyComponentType<T> {
		@Override
		public String toString() {
			return id.toString();
		}
	}

	class Builder<T> {
		private final @NotNull Identifier id;
		private final @NotNull Codec<T> codec;
		private @Nullable Initializer<T> initializer;
		private @Nullable SwitchyComponentType.NbtReader<T> nbtReader;
		private @Nullable NbtMutator<T> nbtMutator;
		private @Nullable EmptyChecker<T> emptyChecker;
		private @Nullable TextProvider<T> textProvider;
		private @Nullable ArgumentEditor<T> argumentEditor;

		public Builder(@NotNull Identifier id, @NotNull Codec<T> codec) {
			this.id = id;
			this.codec = codec;
		}

		public Builder<T> initializer(@Nullable Initializer<T> initializer) {
			this.initializer = initializer;
			return this;
		}

		public Builder<T> reader(@Nullable SwitchyComponentType.NbtReader<T> nbtReader) {
			this.nbtReader = nbtReader;
			return this;
		}

		public Builder<T> nbtMutator(@Nullable NbtMutator<T> nbtMutator) {
			this.nbtMutator = nbtMutator;
			return this;
		}

		public Builder<T> emptyChecker(@Nullable EmptyChecker<T> emptyChecker) {
			this.emptyChecker = emptyChecker;
			return this;
		}

		public Builder<T> textProvider(@Nullable TextProvider<T> textProvider) {
			this.textProvider = textProvider;
			return this;
		}

		public Builder<T> argumentEditor(@Nullable ArgumentEditor<T> argumentEditor) {
			this.argumentEditor = argumentEditor;
			return this;
		}

		public Builder<T> nbtSwitcher(String nbtPath) {
			NbtSwitcher<T> switcher;
			try {
				switcher = new NbtSwitcher<>(NbtPathArgumentType.NbtPath.parse(nbtPath), codec);
			} catch (CommandSyntaxException e) {
				throw new RuntimeException(e);
			}
			this.nbtReader = switcher;
			this.nbtMutator = switcher;
			this.initializer = new DefaultPlayerInitializer<>(switcher);
			return this;
		}

		public SwitchyComponentType<T> build() {
			return new SwitchyComponentType.SimpleSwitchyComponentType<>(
				this.id,
				this.codec,
				this.initializer,
				this.nbtReader,
				this.nbtMutator,
				this.emptyChecker,
				this.textProvider,
				this.argumentEditor
			);
		}
	}
}

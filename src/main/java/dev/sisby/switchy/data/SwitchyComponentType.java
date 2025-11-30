package dev.sisby.switchy.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.SwitchyCommands;
import dev.sisby.switchy.exception.ComponentFailedInitializeException;
import dev.sisby.switchy.exception.NbtException;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface SwitchyComponentType<T> extends TypeRegistry.Type {
	static <T> SwitchyComponentType.Builder<T> builder(@NotNull Identifier id, @NotNull Codec<T> codec) {
		return new SwitchyComponentType.Builder<>(id, codec);
	}

	@NotNull Codec<T> codec();

	@Nullable Initializer<T> initializer();

	@Nullable NbtReader<T> nbtReader();

	@Nullable NbtMutator<T> nbtMutator();

	@Nullable PlayerReader<T> playerReader();

	@Nullable PlayerMutator<T> playerMutator();

	@Nullable EmptyChecker<T> emptyChecker();

	@Nullable TextProvider<T> textProvider();

	@Nullable ArgumentEditor<T> argumentEditor();

	@Nullable Identifier group();

	boolean hidden();

	boolean importable();

	int previewPriority();

	default <S> Optional<S> encode(DynamicOps<S> ops, SwitchyComponentMap components) {
		return codec().encodeStart(ops, components.get(this)).resultOrPartial(Switchy.LOGGER::error);
	}

	default <S> void decode(DynamicOps<S> ops, S input, SwitchyComponentMap components) {
		codec().decode(ops, input).resultOrPartial(Switchy.LOGGER::error).ifPresent(p -> components.set(this, p.getFirst()));
	}

	default void tryInitialize(Collection<SwitchyComponentMap> consumer, NbtCompound nbt, ServerPlayerEntity player, String profileId) {
		Initializer<T> initializer = initializer();
		if (initializer == null) return;
		T value = initializer.initialize(nbt, player, profileId); // value might be null (means "erase key")
		consumer.forEach(c -> c.set(this, value));
	}

	default void tryMutate(SwitchyComponentMap components, NbtCompound playerData, ServerPlayerEntity player) throws NbtException {
		NbtMutator<T> nbtMutator = nbtMutator();
		PlayerMutator<T> playerMutator = playerMutator();
		if (nbtMutator != null) {
			nbtMutator.mutate(player.getServer().getRegistryManager(), components.get(this), playerData);
		} else if (playerMutator != null) {
			playerMutator.mutate(components.get(this), player);
		}
	}

	default MutableText asText(T value) {
		TextProvider<T> textProvider = textProvider();
		if (textProvider != null) {
			return textProvider.toText(value).copy();
		} else {
			return Text.literal(Objects.toString(value));
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
		T initialize(NbtCompound playerNbt, ServerPlayerEntity player, String profileId) throws ComponentFailedInitializeException;
	}

	@FunctionalInterface
	interface NbtReader<T> {
		T read(DynamicRegistryManager registryManager, NbtCompound nbt) throws NbtException;
	}

	@FunctionalInterface
	interface NbtMutator<T> {
		void mutate(DynamicRegistryManager registryManager, T value, NbtCompound nbt) throws NbtException;
	}

	@FunctionalInterface
	interface PlayerReader<T> {
		T read(ServerPlayerEntity player, String profileId);
	}

	@FunctionalInterface
	interface PlayerMutator<T> {
		void mutate(T value, ServerPlayerEntity player);
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
			return value == null ? Text.empty() : provider.apply(value);
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

	record CopyInitializer<T>(NbtReader<T> nbtReader) implements Initializer<T> {
		@Override
		public T initialize(NbtCompound playerNbt, ServerPlayerEntity player, String profileId) throws ComponentFailedInitializeException {
			try {
				return nbtReader.read(player.getServer().getRegistryManager(), playerNbt);
			} catch (Exception e) {
				throw new ComponentFailedInitializeException("", e);
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
		public T read(DynamicRegistryManager registryManager, NbtCompound nbt) throws NbtException {
			try {
				DataResult<T> result = codec.parse(NbtOps.INSTANCE, nbtPath.get(nbt).get(0));
				if (result.error().isPresent()) {
					throw new NbtException("Failed to read from serialized player! %s".formatted(result.error().get().message()));
				}
				return result.resultOrPartial(Switchy.LOGGER::error).orElse(null);
			} catch (CommandSyntaxException e) {
				return null;
			}
		}

		@Override
		public void mutate(DynamicRegistryManager registryManager, T value, NbtCompound nbt) throws NbtException {
			try {
				if (value == null) { // special case - erase the key.
					nbtPath.remove(nbt);
					return;
				}
				DataResult<NbtElement> result = codec.encodeStart(NbtOps.INSTANCE, value);
				if (result.error().isPresent()) {
					throw new NbtException("Failed to serialize component! %s".formatted(result.error().get().message()));
				}
				NbtElement encoded = result.resultOrPartial(Switchy.LOGGER::error).orElse(null);
				if (encoded == null) { // special case - erase the key.
					nbtPath.remove(nbt);
					return;
				}
				nbtPath.put(nbt, encoded);
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
		@Nullable NbtReader<T> nbtReader,
		@Nullable NbtMutator<T> nbtMutator,
		@Nullable PlayerReader<T> playerReader,
		@Nullable PlayerMutator<T> playerMutator,
		@Nullable EmptyChecker<T> emptyChecker,
		@Nullable TextProvider<T> textProvider,
		@Nullable ArgumentEditor<T> argumentEditor,
		@Nullable Identifier group,
		boolean hidden,
		boolean importable,
		int previewPriority
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
		private @Nullable NbtReader<T> nbtReader;
		private @Nullable NbtMutator<T> nbtMutator;
		private @Nullable PlayerReader<T> playerReader;
		private @Nullable PlayerMutator<T> playerMutator;
		private @Nullable EmptyChecker<T> emptyChecker;
		private @Nullable TextProvider<T> textProvider;
		private @Nullable ArgumentEditor<T> argumentEditor;
		private @Nullable Identifier group;
		private boolean hidden = false;
		private boolean importable = false;
		private int previewPriority = 0;

		public Builder(@NotNull Identifier id, @NotNull Codec<T> codec) {
			this.id = id;
			this.codec = codec;
		}

		public Builder<T> initializer(@Nullable Initializer<T> initializer) {
			if (initializer != null) this.initializer = initializer;
			return this;
		}

		public Builder<T> playerReader(@Nullable PlayerReader<T> playerReader) {
			this.playerReader = playerReader;
			return this;
		}

		public Builder<T> playerMutator(@Nullable PlayerMutator<T> playerMutator) {
			this.playerMutator = playerMutator;
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

		public Builder<T> nbtSwitcher(NbtPathArgumentType.NbtPath path) {
			NbtSwitcher<T> switcher = new NbtSwitcher<>(path, codec);
			this.nbtReader = switcher;
			this.nbtMutator = switcher;
			this.initializer = new CopyInitializer<>(switcher);
			return this;
		}

		public Builder<T> group(@Nullable Identifier group) {
			this.group = group;
			return this;
		}

		public Builder<T> hidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		public Builder<T> importable(boolean importable) {
			this.importable = importable;
			return this;
		}

		public Builder<T> previewPriority(int priority) {
			this.previewPriority = priority;
			return this;
		}

		public SwitchyComponentType<T> build() {
			return new SwitchyComponentType.SimpleSwitchyComponentType<>(
				this.id,
				this.codec,
				this.initializer,
				this.nbtReader,
				this.nbtMutator,
				this.playerReader,
				this.playerMutator,
				this.emptyChecker,
				this.textProvider,
				this.argumentEditor,
				this.group,
				this.hidden,
				this.importable,
				this.previewPriority
			);
		}
	}
}

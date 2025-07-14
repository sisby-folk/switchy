package dev.sisby.switchy.data;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface SwitchyComponentHolder<H extends SwitchyComponentHolder<H>> {
	SwitchyComponentMap components();

	@Nullable
	default <T> T get(SwitchyComponentType<? extends T> type) {
		return this.components().get(type);
	}

	default <T> T getOrDefault(SwitchyComponentType<? extends T> type, T fallback) {
		return this.components().getOrDefault(type, fallback);
	}

	default <T> T getOrGetDefault(SwitchyComponentType<? extends T> type, Function<H, T> fallback) {
		return this.components().getOrDefault(type, fallback.apply((H) this));
	}

	@Nullable
	default <T> T set(SwitchyComponentType<T> type, @Nullable T value) {
		return this.components().set(type, value);
	}

	@Nullable
	default <T, U> T apply(SwitchyComponentType<T> type, T defaultValue, U change, BiFunction<T, U, T> applier) {
		return this.set(type, applier.apply(this.getOrDefault(type, defaultValue), change));
	}

	@Nullable
	default <T> T apply(SwitchyComponentType<T> type, T defaultValue, UnaryOperator<T> applier) {
		T object = this.getOrDefault(type, defaultValue);
		return this.set(type, applier.apply(object));
	}

	@Nullable
	default <T> T remove(SwitchyComponentType<? extends T> type) {
		return this.components().remove(type);
	}

	default boolean contains(SwitchyComponentType<?> type) {
		return this.components().contains(type);
	}
}

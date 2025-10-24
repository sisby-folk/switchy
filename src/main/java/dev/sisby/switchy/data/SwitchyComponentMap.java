package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchyComponentMap {
	public static final Codec<SwitchyComponentMap> CODEC = SwitchyComponentType.TYPE_TO_VALUE_MAP_CODEC.flatComapMap(SwitchyComponentMap::create, map -> DataResult.success(new Reference2ObjectArrayMap<>(map.map)));
	private final Reference2ObjectMap<SwitchyComponentType<?>, Object> map;

	public static SwitchyComponentMap empty() {
		return create(Map.of());
	}

	private static SwitchyComponentMap create(Map<SwitchyComponentType<?>, Object> components) {
		return new SwitchyComponentMap(new Reference2ObjectArrayMap<>(components));
	}

	private SwitchyComponentMap(Reference2ObjectMap<SwitchyComponentType<?>, Object> map) {
		this.map = map;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T get(SwitchyComponentType<? extends T> type) {
		return (T) this.map.get(type);
	}

	public boolean contains(SwitchyComponentType<?> type) {
		return this.map.containsKey(type);
	}

	public <T> T getOrDefault(SwitchyComponentType<? extends T> type, T fallback) {
		T object = this.get(type);
		return object != null ? object : fallback;
	}

	public Set<SwitchyComponentType<?>> keySet() {
		return this.map.keySet();
	}

	public int size() {
		return this.map.size();
	}

	public String toString() {
		return map.entrySet().stream().map(e -> "%s: %s".formatted(e.getKey().id().getPath(), e.getValue().toString())).collect(Collectors.joining("\n"));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T set(SwitchyComponentType<? extends T> type, @Nullable T value) {
		return (T) this.map.put(type, value);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T remove(SwitchyComponentType<? extends T> type) {
		return (T) this.map.remove(type);
	}

	public List<MutableText> asTexts() {
		return keySet().stream().map(t -> Text.empty().append(Text.literal(t.id().getPath() + ": ").formatted(Formatting.GRAY)).append(t.asText(this))).toList();
	}
}

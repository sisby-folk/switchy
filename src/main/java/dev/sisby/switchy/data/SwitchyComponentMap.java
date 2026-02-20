package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchyComponentMap {
	public static Codec<SwitchyComponentMap> codec(SwitchyComponentTypes types) {
		return types.TYPE_TO_VALUE_MAP_CODEC.flatComapMap(SwitchyComponentMap::create, map -> DataResult.success(new Reference2ObjectArrayMap<>(map.map)));
	}

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
		return map.entrySet().stream().map(e -> "%s: %s".formatted(e.getKey().id().getPath(), Objects.toString(e.getValue()))).collect(Collectors.joining("\n"));
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

	public List<MutableText> asTexts(MinecraftServer server) {
		return SwitchyComponentTypes.grouped(keySet()).entrySet().stream().filter(e -> !e.getValue().stream().allMatch(t -> t.hidden() || this.get(t) == null || (t.emptyChecker() != null && !t.isPrecious(this)))).map(e -> Text.empty()
				.append(Text.literal(e.getKey().getPath() + ": ").formatted(Formatting.GRAY))
				.append(Texts.join(e.getValue().stream().filter(t -> !t.hidden() && this.get(t) != null && (t.emptyChecker() == null || t.isPrecious(this))).map(t -> t.asText(server, this)).toList(), Text.literal(", ").formatted(Formatting.GRAY)))
		).toList();
	}
}

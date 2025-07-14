package dev.sisby.switchy.data;

import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public interface SwitchyLayerType extends TypeRegistry.Type {
	static SwitchyLayerType.Builder builder(@NotNull Identifier id) {
		return new SwitchyLayerType.Builder(id);
	}

	class Builder {
		@NotNull
		private final Identifier id;

		private Builder(@NotNull Identifier id) {
			this.id = id;
		}

		public SwitchyLayerType build() {
			return new SimpleSwitchyLayerType(id);
		}

		record SimpleSwitchyLayerType(Identifier id) implements SwitchyLayerType {
			@Override
			public String toString() {
				return id.toString();
			}
		}
	}
}

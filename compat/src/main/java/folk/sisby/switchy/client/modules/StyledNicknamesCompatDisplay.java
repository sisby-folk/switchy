package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class StyledNicknamesCompatDisplay implements SwitchyDisplayModule {
	public static final Identifier ID = new Identifier("switchy",  "styled_nicknames");

	public @Nullable Text styled_nickname;

	public static final String KEY_NICKNAME = "styled_nickname";

	@Override
	public Pair<Component, SwitchScreenPosition> getDisplayComponent() {
		if (styled_nickname == null) return null;
		return Pair.of(Components.label(styled_nickname), SwitchScreenPosition.LEFT);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (styled_nickname != null) {
			outNbt.putString(KEY_NICKNAME, Text.Serializer.toJsonTree(styled_nickname).getAsString());
		}
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		if (nbt.contains(KEY_NICKNAME)) styled_nickname = Text.Serializer.fromJson(nbt.getString(KEY_NICKNAME));
	}

	public static void touch() {}

	static {
		SwitchyDisplayModuleRegistry.registerModule(ID, StyledNicknamesCompatDisplay::new);
	}
}

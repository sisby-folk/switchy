package folk.sisby.switchy.modules;

import com.mojang.datafixers.util.Pair;
import eu.pb4.placeholders.api.TextParserUtils;
import folk.sisby.switchy.SwitchyDisplayModules;
import folk.sisby.switchy.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.screen.SwitchScreen.ComponentPosition;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class StyledNicknamesCompatDisplay implements SwitchyDisplayModule<StyledNicknamesCompat> {
	public static final Identifier ID = new Identifier("switchy",  "styled_nicknames");

	public @Nullable Text nicknameText;

	public static final String KEY_NICKNAME = "nicknameText";

	@Override
	public void fillFromData(StyledNicknamesCompat styledNicknamesCompat) {
		nicknameText = TextParserUtils.formatText(styledNicknamesCompat.styled_nickname);
	}

	@Override
	public Pair<Component, ComponentPosition> getDisplayComponent() {
		if (nicknameText == null) return null;
		return Pair.of(Components.label(nicknameText), ComponentPosition.LEFT);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (nicknameText != null) {
			outNbt.putString(KEY_NICKNAME, Text.Serializer.toJsonTree(nicknameText).getAsString());
		}
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		if (nbt.contains(KEY_NICKNAME)) nicknameText = Text.Serializer.fromJson(nbt.getString(KEY_NICKNAME));
	}

	public static void touch() {}

	static {
		SwitchyDisplayModules.registerModule(ID, StyledNicknamesCompatDisplay::new);
	}
}

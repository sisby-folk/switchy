package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.ui.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.ui.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

/**
 * The client-displayable variant of a module that switches nicknames from Patbox's Styled Nicknames.
 *
 * @author Sisby folk
 * @see SwitchyDisplayModule
 * @see folk.sisby.switchy.modules.StyledNicknamesCompat
 * @since 2.0.0
 */
@ClientOnly
public class StyledNicknamesCompatDisplay implements SwitchyClientModule,  SwitchyDisplayModule {
	/**
	 * Identifier for this module.
	 * Must match {@link folk.sisby.switchy.modules.StyledNicknamesCompat}.
	 */
	public static final Identifier ID = new Identifier("switchy", "styled_nicknames");
	/**
	 * The NBT key where the nickname (in serialized text format) is stored.
	 * Must match {@link folk.sisby.switchy.modules.StyledNicknamesCompat#toClientNbt()}.
	 */
	public static final String KEY_NICKNAME = "styled_nickname";

	static {
		SwitchyClientModuleRegistry.registerModule(ID, StyledNicknamesCompatDisplay::new);
	}

	/**
	 * The styled nickname, in Text format.
	 */
	public @Nullable Text styled_nickname;

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent() {
		if (styled_nickname == null) return null;
		return Pair.of(Components.label(styled_nickname), SwitchySwitchScreenPosition.LEFT);
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
}

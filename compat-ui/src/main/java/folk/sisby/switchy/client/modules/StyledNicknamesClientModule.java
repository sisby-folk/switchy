package folk.sisby.switchy.client.modules;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.modules.StyledNicknamesModule;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import folk.sisby.switchy.ui.api.module.SwitchyUIModule;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The client-displayable variant of a module that switches nicknames from Patbox's Styled Nicknames.
 *
 * @author Sisby folk
 * @see SwitchyUIModule
 * @see StyledNicknamesModule
 * @since 2.0.0
 */
public class StyledNicknamesClientModule implements SwitchyClientModule, SwitchyUIModule {
	/**
	 * Identifier for this module.
	 * Must match {@link StyledNicknamesModule}.
	 */
	public static final Identifier ID = Identifier.of("switchy", "styled_nicknames");
	/**
	 * The NBT key where the nickname (in serialized text format) is stored.
	 * Must match {@link StyledNicknamesModule#toClientNbt()}.
	 */
	public static final String KEY_NICKNAME = "styled_nickname";
	/**
	 * The styled nickname, in Text format.
	 */
	public @Nullable Text styled_nickname;

	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyClientModuleRegistry.registerModule(ID, StyledNicknamesClientModule::new);
	}

	@Override
	public Pair<Component, SwitchyUIPosition> getPreviewComponent(String presetName) {
		if (styled_nickname == null) return null;
		return Pair.of(Components.label(styled_nickname), SwitchyUIPosition.LEFT);
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		if (styled_nickname != null) {
			outNbt.put(KEY_NICKNAME, TextCodecs.STRINGIFIED_CODEC.encodeStart(NbtOps.INSTANCE, styled_nickname).getOrThrow());
		}
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		if (nbt.contains(KEY_NICKNAME)) styled_nickname = TextCodecs.STRINGIFIED_CODEC.decode(NbtOps.INSTANCE, nbt.get(KEY_NICKNAME)).getOrThrow().getFirst();
	}
}

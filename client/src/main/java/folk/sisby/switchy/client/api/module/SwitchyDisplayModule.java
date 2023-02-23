package folk.sisby.switchy.client.api.module;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import io.wispforest.owo.ui.core.Component;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

@ClientOnly
public interface SwitchyDisplayModule extends SwitchySerializable {
	@Nullable Pair<Component, SwitchySwitchScreenPosition> getDisplayComponent();
}

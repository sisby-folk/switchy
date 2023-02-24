package folk.sisby.switchy.api.module.presets;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.presets.SwitchyPresetData;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.util.Identifier;

import java.util.Map;

public interface SwitchyDisplayPreset extends SwitchyPresetData<SwitchyDisplayModule> {
	Map<Identifier, Pair<Component, SwitchySwitchScreenPosition>> getDisplayComponents();
}

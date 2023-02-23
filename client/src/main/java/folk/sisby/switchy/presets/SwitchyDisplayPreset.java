package folk.sisby.switchy.presets;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ClientOnly
public class SwitchyDisplayPreset extends SwitchyPresetData<SwitchyDisplayModule> {
	public SwitchyDisplayPreset(String presetName, Map<Identifier, Boolean> modules) {
		super(presetName, SwitchyDisplayModuleRegistry.MODULE_SUPPLIERS.entrySet().stream()
				.filter(e -> modules.containsKey(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
	}

	public Map<Identifier, Pair<Component, SwitchySwitchScreenPosition>> getDisplayComponents() {
		Map<Identifier, Pair<Component, SwitchySwitchScreenPosition>> map = new HashMap<>();
		modules.forEach((id, module) -> {
			@Nullable Pair<Component, SwitchySwitchScreenPosition> component = module.getDisplayComponent();
			if (component != null) {
				map.put(id, component);
			}
		});
		return map;
	}
}

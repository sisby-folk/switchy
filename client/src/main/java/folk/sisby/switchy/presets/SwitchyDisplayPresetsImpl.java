package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.module.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;

@ClientOnly
public class SwitchyDisplayPresetsImpl extends SwitchyPresetsDataImpl<SwitchyDisplayModule, SwitchyDisplayPreset> implements SwitchyDisplayPresets {
	public SwitchyDisplayPresetsImpl() {
		super(new HashMap<>(), SwitchyDisplayPresetImpl::new, SwitchyDisplayModuleRegistry::supplyModule, true, SwitchyClient.LOGGER);
	}

	@Override
	public // Don't Log. Don't check for existence. `modules` is expected to be desync'd from the DisplayModules.
	void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		list.forEach((e) -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null) {
				getModules().put(id, enabled);
			}
		});
	}
}

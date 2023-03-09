package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.api.module.SwitchyDisplayModuleRegistry;
import folk.sisby.switchy.util.PresetConverter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sisby folk
 * @see SwitchyDisplayPresets
 * @since 1.9.1
 */
@ClientOnly
public class SwitchyDisplayPresetsImpl extends SwitchyPresetsDataImpl<SwitchyDisplayModule, SwitchyDisplayPreset> implements SwitchyDisplayPresets {
	final Map<Identifier, SwitchyModuleInfo> moduleInfo;

	int permissionLevel;

	/**
	 * Returns an empty display presets object.
	 *
	 * @param moduleInfo a map of module info by module ID.
	 * @param permissionLevel the permission level for the player.
	 */
	public SwitchyDisplayPresetsImpl(Map<Identifier, SwitchyModuleInfo> moduleInfo, int permissionLevel) {
		super(new HashMap<>(), SwitchyDisplayPresetImpl::new, SwitchyDisplayModuleRegistry::supplyModule,true, SwitchyClient.LOGGER);
		this.moduleInfo = moduleInfo;
		this.permissionLevel = permissionLevel;
	}

	@Override
	void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		// Don't Log. Don't check for existence. `modules` is expected to be desync'd from the DisplayModules.
		list.forEach((e) -> {
			Identifier id;
			if ((id = Identifier.tryParse(e.asString())) != null) {
				getModules().put(id, enabled);
			}
		});
	}

	@Override
	public Map<Identifier, SwitchyModuleInfo> getModuleInfo() {
		return moduleInfo;
	}

	public int getPermissionLevel() {
		return permissionLevel;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		super.fillFromNbt(nbt);
		moduleInfo.putAll(SwitchyModuleRegistry.infoFromNbt(nbt.getCompound(PresetConverter.KEY_MODULE_INFO)));
		permissionLevel = nbt.getInt(PresetConverter.KEY_PERMISSION_LEVEL);
	}

	@Override
	public void enableModule(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		if (!containsModule(id)) throw new ModuleNotFoundException();
		getModules().put(id, true);
	}

	@Override
	public void disableModule(Identifier id) throws ModuleNotFoundException, IllegalStateException {
		if (!containsModule(id)) throw new ModuleNotFoundException();
		getModules().put(id, false);
	}
}

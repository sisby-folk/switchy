package folk.sisby.switchy.presets;

import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.module.presets.SwitchyClientPreset;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.util.PresetConverter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sisby folk
 * @see SwitchyClientPresets
 * @since 1.9.1
 */
public class SwitchyClientPresetsImpl extends SwitchyPresetsDataImpl<SwitchyClientModule, SwitchyClientPreset> implements SwitchyClientPresets {
	final Map<Identifier, SwitchyModuleInfo> moduleInfo;

	int permissionLevel;

	/**
	 * Returns an empty client presets object.
	 *
	 * @param moduleInfo      a map of module info by module ID.
	 * @param permissionLevel the permission level for the player.
	 */
	public SwitchyClientPresetsImpl(Map<Identifier, SwitchyModuleInfo> moduleInfo, int permissionLevel) {
		super(new HashMap<>(), true, SwitchyClient.LOGGER);
		this.moduleInfo = moduleInfo;
		this.permissionLevel = permissionLevel;
	}

	@Override
	public SwitchyClientPreset constructPreset(String name, Map<Identifier, Boolean> modules) {
		return new SwitchyClientPresetImpl(name, modules);
	}

	@Override
	public SwitchyClientModule supplyModule(Identifier id) {
		return SwitchyClientModuleRegistry.supplyModule(id);
	}

	@Override
	public @Nullable SwitchySerializable supplyModuleConfig(Identifier id) {
		return moduleInfo.containsKey(id) && moduleInfo.get(id).moduleConfig() != null ? moduleInfo.get(id).moduleConfig().get() : null;
	}

	@Override
	void toggleModulesFromNbt(NbtList list, Boolean enabled, Boolean silent) {
		// Don't Log. Don't check for existence. `modules` is expected to be desync'd from the actual modules.
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

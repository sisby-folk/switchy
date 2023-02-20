package folk.sisby.switchy.api.module;

public interface SwitchyDisplayModuleData<Module extends SwitchyModuleData> extends SwitchyModuleData {
	/**
	 * Only runs on the server
	 * Any data you need to transform using server mods before serializing - do it here.
	 */
	void fillFromData(Module module);
}

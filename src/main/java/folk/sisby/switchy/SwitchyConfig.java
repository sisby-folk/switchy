package folk.sisby.switchy;

import folk.sisby.switchy.api.ModuleImportable;
import org.quiltmc.config.api.WrappedConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.ValueMap;

public class SwitchyConfig extends WrappedConfig {
	@Comment("Permission for importing various modules")
	@Comment("Allowed: Non-operators can import this module, and it's imported by default")
	@Comment("Operator: Operators can import this module by flagging it in the import command")
	public final ValueMap<ModuleImportable> moduleImportable = ValueMap.builder(ModuleImportable.OPERATOR).build();

	@Comment("Permission for importing various modules")
	@Comment("These values are read-only - changing them here has no effect.")
	@Comment("Always_Allowed: Non-operators can import this module")
	@Comment("Never: This module cannot be imported")
	public final ValueMap<ModuleImportable> moduleImportableReadOnly = ValueMap.builder(ModuleImportable.NEVER).build();
}

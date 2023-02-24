package folk.sisby.switchy;

import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import org.quiltmc.config.api.WrappedConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.ValueMap;

/**
 * @author Sisby folk
 * @since 1.7.1
 * Handles module cold-editing permission configuration (including importing). Saved as {@code config/switchy/config.toml}
 */
public class SwitchyConfig extends WrappedConfig {
	@Comment("Permission for editing module data")
	@Comment("Allowed: Non-operators can import this module, and it's imported by default")
	@Comment("Operator: Operators can import this module using command flags")
	public final ValueMap<SwitchyModuleEditable> moduleEditable = ValueMap.builder(SwitchyModuleEditable.OPERATOR).build();

	@Comment("Permission for editing module data")
	@Comment("These values are read-only - changing them here has no effect.")
	@Comment("Always_Allowed: Non-operators can import this module")
	@Comment("Never: This module cannot be imported")
	public final ValueMap<SwitchyModuleEditable> moduleEditableReadOnly = ValueMap.builder(SwitchyModuleEditable.NEVER).build();
}

package folk.sisby.switchy;

import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;

/**
 * Handles module cold-editing permission configuration (including importing).
 * Saved as {@code config/switchy/config.toml}.
 *
 * @author Sisby folk
 * @since 1.7.1
 */
public class SwitchyConfig extends WrappedConfig {
	/**
	 * Editable list of ALLOWED/OPERATOR {@link SwitchyModuleEditable} values for relevant registered module IDs.
	 */
	@Comment("Permission for editing module data")
	@Comment("Allowed: Non-operators can import this module, and it's imported by default")
	@Comment("Operator: Operators can import this module using command flags")
	public final ValueMap<SwitchyModuleEditable> moduleEditable = ValueMap.builder(SwitchyModuleEditable.OPERATOR).build();

	/**
	 * Read-only list of ALWAYS_ALLOWED/NEVER {@link SwitchyModuleEditable} values for relevant registered module IDs.
	 */
	@Comment("Permission for editing module data")
	@Comment("These values are read-only - changing them here has no effect.")
	@Comment("Always_Allowed: Non-operators can import this module")
	@Comment("Never: This module cannot be imported")
	public final ValueMap<SwitchyModuleEditable> moduleEditableReadOnly = ValueMap.builder(SwitchyModuleEditable.NEVER).build();
}

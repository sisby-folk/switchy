package folk.sisby.switchy.config;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * Handles scale-type configuration for the Pehkui module.
 * Saved as {@code config/switchy/pehkui.toml}.
 *
 * @author Sisby folk
 * @since 2.2.0
 */
public class PehkuiModuleConfig extends WrappedConfig {
	/**
	 * Editable list of scale types that the Pehkui module will switch.
	 */
	@Comment("Scale types that the pehkui module will switch on this server.")
	public final ValueList<String> scaleTypes = ValueList.create(
		ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, ScaleTypes.HEIGHT).toString(),
		ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, ScaleTypes.HEIGHT).toString(),
		ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, ScaleTypes.WIDTH).toString(),
		ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, ScaleTypes.MODEL_HEIGHT).toString(),
		ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, ScaleTypes.MODEL_WIDTH).toString()
	);
}

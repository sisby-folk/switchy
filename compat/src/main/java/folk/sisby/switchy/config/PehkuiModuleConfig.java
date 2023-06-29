package folk.sisby.switchy.config;

import org.quiltmc.config.api.WrappedConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.ValueList;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * Handles module cold-editing permission configuration (including importing).
 * Saved as {@code config/switchy/config.toml}.
 *
 * @author Sisby folk
 * @since 2.2.0
 */
public class PehkuiModuleConfig extends WrappedConfig {
	/**
	 * Editable list of scale types that the Pehkui module will switch .
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

package folk.sisby.switchy.config;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import io.github.apace100.apoli.power.Power;

/**
 * Handles module cold-editing permission configuration (including importing).
 * Saved as {@code config/switchy/config.toml}.
 *
 * @author Sisby folk
 * @since 2.3.0
 */
public class ApoliModuleConfig extends WrappedConfig {
	/**
	 * Whether to switch the presence of powers added by /power
	 */
	@Comment("Whether to switch the presence of powers added by /power")
	public final Boolean switchCommandPowers = false;

	/**
	 * A list of power IDs that are an exception to the above rule
	 */
	@Comment("A list of power IDs that are an exception to the above rule")
	public final ValueList<String> exceptionPowerIds = ValueList.create("");

	/**
	 * @param type a power type.
	 * @return whether the power can be switched by the apoli module.
	 */
	public boolean canSwitchPower(Power type) {
		return (switchCommandPowers && !exceptionPowerIds.contains(type.getId().toString())) || (!switchCommandPowers && exceptionPowerIds.contains(type.getId().toString()));
	}
}

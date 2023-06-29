package folk.sisby.switchy.config;

import io.github.apace100.apoli.power.PowerType;
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
	public boolean canSwitchPower(PowerType<?> type) {
		return (switchCommandPowers && !exceptionPowerIds.contains(type.getIdentifier().toString())) || (!switchCommandPowers && exceptionPowerIds.contains(type.getIdentifier().toString()));
	}
}

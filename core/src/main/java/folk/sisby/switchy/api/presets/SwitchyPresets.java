package folk.sisby.switchy.api.presets;

import folk.sisby.switchy.api.module.SwitchyModule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Sisby folk
 * @since 1.0.0
 * @see SwitchyPresetsData
 * @see folk.sisby.switchy.api.SwitchyPlayer
 *
 */
public interface SwitchyPresets extends SwitchyPresetsData<SwitchyModule, SwitchyPreset> {
	void importFromOther(ServerPlayerEntity player, Map<String, SwitchyPreset> other);

	void importFromOther(ServerPlayerEntity player, SwitchyPresets other);

	String switchCurrentPreset(ServerPlayerEntity player, String name) throws IllegalArgumentException, IllegalStateException;

	void saveCurrentPreset(ServerPlayerEntity player);

	/**
	 * Allows you to modify the data associated with the current preset for a specified module, by saving it, mutating it, then loading it all in one swoop.
	 **/
	void duckCurrentModule(ServerPlayerEntity player, Identifier id, Consumer<SwitchyModule> mutator) throws IllegalArgumentException, IllegalStateException;
}

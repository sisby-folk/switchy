package folk.sisby.switchy.presets;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.module.presets.SwitchyClientPreset;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.Map;

/**
 * @author Sisby folk
 * @see SwitchyClientPreset
 * @since 1.9.1
 */
@ClientOnly
public class SwitchyClientPresetImpl extends SwitchyPresetDataImpl<SwitchyClientModule> implements SwitchyClientPreset {
	/**
	 * Constructs an instance of the object.
	 *
	 * @param name    the desired name for the new preset.
	 * @param modules the enabled status of modules from the client presets object.
	 * @throws InvalidWordException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 */
	public SwitchyClientPresetImpl(String name, Map<Identifier, Boolean> modules) throws InvalidWordException {
		super(name, modules, SwitchyClientModuleRegistry::supplyModule);
	}
}

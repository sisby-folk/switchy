package folk.sisby.switchy.presets;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static folk.sisby.switchy.presets.SwitchyPresets.*;

@ClientOnly
public class SwitchyDisplayPresets {
	public final Map<String, SwitchyDisplayPreset> presets;
	public final Map<Identifier, Boolean> modules;
	public final String currentPreset;

	public SwitchyDisplayPresets(Map<String, SwitchyDisplayPreset> presets, Map<Identifier, Boolean> modules, String currentPreset) {
		this.presets = presets;
		this.modules = modules;
		this.currentPreset = currentPreset;
	}

	// Highly derivative of SwitchyPresets. Might be able to reuse code somehow.
	public static SwitchyDisplayPresets fromNbt(NbtCompound nbt) {
		Map<Identifier, Boolean> modules = new HashMap<>();

		nbt.getList(KEY_PRESET_MODULE_DISABLED, NbtElement.STRING_TYPE).forEach(e -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null) {
				modules.put(id, false);
			}
		});
		nbt.getList(KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).forEach(e -> {
			Identifier id;
			if (e instanceof NbtString s && (id = Identifier.tryParse(s.asString())) != null) {
				modules.put(id, true);
			}
		});

		NbtCompound presetList = nbt.getCompound(KEY_PRESET_LIST);
		Map<String, SwitchyDisplayPreset> presets = presetList.getKeys().stream().collect(Collectors.toMap(
				key -> key,
				key -> SwitchyDisplayPreset.fromNbt(key, presetList.getCompound(key), modules.keySet())
		));

		return new SwitchyDisplayPresets(presets, modules, nbt.getString(SwitchyPresets.KEY_PRESET_CURRENT));
	}
}

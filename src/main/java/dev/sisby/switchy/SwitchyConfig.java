package dev.sisby.switchy;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.ChangeWarning;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.TreeMap;

public class SwitchyConfig extends WrappedConfig {
	@Comment("The texture ID avatar renderer to use in exports (for use in e.g. Utter)")
	public String exportAvatarUrl = "https://vzge.me/bust/256/%s";

	@Comment("Toggles specific components instance-wide.")
	@Comment("Enabled components can be disabled for just you via '/switchy components'")
	@Comment("Disabling components on an existing save will cause data loss!")
	@ChangeWarning(folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type.RequiresRestart)
	public Map<String, Boolean> components = ValueMap.builder(true).build();

	public boolean isEnabled(Identifier id, boolean dataEnabled) {
		components.putIfAbsent(id.toString(), dataEnabled);
		Map<String, Boolean> sorted = new TreeMap<>(components);
		components.clear();
		components.putAll(sorted);
		return components.get(id.toString());
	}
}

package dev.sisby.switchy;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;

public class SwitchyConfig extends WrappedConfig {
	@Comment("The texture ID avatar renderer to use in exports (for use in e.g. Utter)")
	public String exportAvatarUrl = "https://vzge.me/bust/256/%s";
}

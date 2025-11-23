package dev.sisby.switchy;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;

public class SwitchyConfig extends WrappedConfig {
	@Comment("Prevents the singleplayer server from shutting down during a kick-style (1.20 and below) switch.")
	@Comment("May cause compatibility issues - if you crash when switching in singleplayer, turn this off.")
	@Comment("Must be enabled to switch as a LAN host.")
	public boolean fastSingleplayerReconnect = true;

	@Comment("The texture ID avatar renderer to use in exports (for use in e.g. Utter)")
	public String exportAvatarUrl = "https://vzge.me/bust/256/%s";
}

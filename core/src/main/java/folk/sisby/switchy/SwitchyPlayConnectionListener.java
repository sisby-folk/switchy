package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.presets.SwitchyPresetsImpl;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Forwards player connection events as Switchy events.
 * Ensures player data is set up on first join.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class SwitchyPlayConnectionListener {
	public static void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		if (presets == null) {
			presets = new SwitchyPresetsImpl(true);
			presets.fillFromNbt(new NbtCompound());
			((SwitchyPlayer) player).switchy$setPresets(presets);
		}
		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				handler.getPlayer().getUuid(), presets.getCurrentPresetName(), null, presets.getEnabledModuleNames()
		);
		SwitchyEvents.SWITCH.invoker().onSwitch(handler.getPlayer(), switchEvent);
	}

	public static void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				handler.getPlayer().getUuid(), null, presets.getCurrentPresetName(), presets.getEnabledModuleNames()
		);
		SwitchyEvents.SWITCH.invoker().onSwitch(handler.getPlayer(), switchEvent);
	}
}

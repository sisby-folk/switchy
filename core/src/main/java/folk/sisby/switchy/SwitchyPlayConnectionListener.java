package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;

public class SwitchyPlayConnectionListener implements ServerPlayConnectionEvents.Join, ServerPlayConnectionEvents.Disconnect {
	@Override
	public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		if (presets == null) {
			presets = new SwitchyPresets(true);
			presets.fillFromNbt(new NbtCompound());
			((SwitchyPlayer) player).switchy$setPresets(presets);
		}
		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				handler.getPlayer().getUuid(), presets.getCurrentPreset().presetName, null, presets.getEnabledModuleNames()
		);
		SwitchyEvents.SWITCH.invoker().onSwitch(handler.getPlayer(), switchEvent);
	}

	@Override
	public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				handler.getPlayer().getUuid(), null, presets.getCurrentPreset().presetName, presets.getEnabledModuleNames()
		);
		SwitchyEvents.SWITCH.invoker().onSwitch(handler.getPlayer(), switchEvent);
	}
}

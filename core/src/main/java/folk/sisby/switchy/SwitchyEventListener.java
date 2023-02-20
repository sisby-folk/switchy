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

public class SwitchyEventListener implements ServerPlayConnectionEvents.Join {
	@Override
	public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		if (presets == null) {
			((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(new NbtCompound(), true));
			presets = ((SwitchyPlayer) player).switchy$getPresets();
		}
		SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
				handler.getPlayer().getUuid(), presets.getCurrentPreset().presetName, null, presets.getEnabledModuleNames()
		);
		SwitchyEvents.SWITCH.invoker().onSwitch(handler.getPlayer(), switchEvent);
	}
}

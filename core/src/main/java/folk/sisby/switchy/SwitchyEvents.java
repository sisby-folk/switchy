package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.SwitchySwitchEvent;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import static folk.sisby.switchy.SwitchyNetworking.S2C_SWITCH;

public class SwitchyEvents {
	public static void InitializeEvents() {
		ServerPlayConnectionEvents.JOIN.register((spn, ps, s) -> {
			ServerPlayerEntity player = spn.getPlayer();
			SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
			if (presets == null) {
				((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(new NbtCompound(), player));
				presets = ((SwitchyPlayer) player).switchy$getPresets();
			}
			SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
					spn.getPlayer().getUuid(), presets.getCurrentPreset().presetName, null, presets.getEnabledModuleNames()
			);
			folk.sisby.switchy.api.SwitchyEvents.fireSwitch(switchEvent);
			if (ServerPlayNetworking.canSend(player, S2C_SWITCH)) {
				ps.sendPacket(S2C_SWITCH, PacketByteBufs.create().writeNbt(switchEvent.toNbt()));
			}
		});
	}
}

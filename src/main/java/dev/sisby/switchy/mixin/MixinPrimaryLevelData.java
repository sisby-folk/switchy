package dev.sisby.switchy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sisby.switchy.duck.SwitchyWorldData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(PrimaryLevelData.class)
public class MixinPrimaryLevelData implements SwitchyWorldData {
	private UUID switchy$override = null;

	@ModifyReturnValue(method = "getSinglePlayerUUID", at = @At("RETURN"))
	private UUID overrideIdSoHostsCanPerformReconfiguration(UUID original) {
		return switchy$override != null ? switchy$override : original;
	}

	@Override
	public void overrideSingleplayerID(UUID id) {
		switchy$override = id;
	}
}

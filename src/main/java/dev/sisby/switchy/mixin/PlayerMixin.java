package dev.sisby.switchy.mixin;

import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayerMixin implements SwitchyPlayer {
	private NbtCompound switchy$hotSwap = null;

	@Override
	public void switchy$hotSwap(NbtCompound nbt) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		switchy$hotSwap = nbt;
		try {
			self.networkHandler.disconnect(Text.of("Switching Profiles"));
		} finally {
			switchy$hotSwap = null;
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	public void applyHotSwapData(NbtCompound nbt, CallbackInfo ci) {
		if (switchy$hotSwap != null) {
			nbt.copyFrom(switchy$hotSwap);
		}
	}
}

package dev.sisby.switchy.mixin;

import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
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
			self.getServer().getPlayerManager().respawnPlayer(self, true, Entity.RemovalReason.DISCARDED);
			self.kill();
		} finally {
			switchy$hotSwap = null;
		}
	}

	@Inject(method = "copyFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setLastDeathPos(Ljava/util/Optional;)V", shift = At.Shift.AFTER), cancellable = true)
	public void executeHotSwap(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (((PlayerMixin) (Object) oldPlayer).switchy$hotSwap != null) {
			oldPlayer.discard();
			self.readNbt(((PlayerMixin) (Object) oldPlayer).switchy$hotSwap);
			ci.cancel(); // Don't let any mods do copyFrom logic
		}
	}
}

package dev.sisby.switchy.mixin;

import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
	@Inject(method = "writeNbt", at = @At("HEAD"), cancellable = true)
	public void applyHotSwapData(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof SwitchyPlayer sp && self instanceof ServerPlayerEntity spe) { // mods will overwrite our hotswap data if we don't do this up here.
			NbtCompound hotSwap = sp.switchy$hotSwapData();
			if (hotSwap != null) {
				if (self.getServer().isHost(spe.getGameProfile())) { // hosts don't support reconfiguration unless we patch this
					self.getServer().getSaveProperties().getPlayerData().getKeys().clear();
					self.getServer().getSaveProperties().getPlayerData().copyFrom(hotSwap);
					sp.switchy$getPlayerData().writeNbt(self.getServer().getSaveProperties().getPlayerData());
				}
				nbt.copyFrom(hotSwap);
				sp.switchy$getPlayerData().writeNbt(nbt);
				cir.setReturnValue(nbt);
				cir.cancel();
			}
		}
	}
}

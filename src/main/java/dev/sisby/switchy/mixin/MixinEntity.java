package dev.sisby.switchy.mixin;

import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.exception.NbtException;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
	@Inject(method = "saveWithoutId", at = @At("HEAD"), cancellable = true)
	public void applyHotSwapData(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof SwitchyPlayer sp && self instanceof ServerPlayer spe) { // mods will overwrite our hotswap data if we don't do this up here.
			CompoundTag hotSwap = sp.switchy$hotSwapData();
			if (hotSwap != null) {
				if (self.getServer().isSingleplayerOwner(spe.getGameProfile())) { // hosts don't support reconfiguration unless we patch this
					CompoundTag levelDat = self.getServer().getWorldData().getLoadedPlayerTag();
					if (levelDat != null) {
						levelDat.getAllKeys().clear();
						levelDat.merge(hotSwap);
						sp.switchy$getPlayerData().writeNbt(spe.getServer().registryAccess(), levelDat);
					}
				}
				nbt.merge(hotSwap);
				sp.switchy$getPlayerData().writeNbt(spe.getServer().registryAccess(), nbt);
				cir.setReturnValue(nbt);
				cir.cancel();
			}
		}
	}

	@Inject(method = "load", at = @At("TAIL"))
	public void cacheDisplayData(CompoundTag nbt, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof SwitchyPlayer sp && self instanceof ServerPlayer spe) {
			if (sp.switchy$getPlayerData() != null) {
				for (String profile : sp.switchy$getPlayerData().keySet()) {
					try {
						sp.switchy$getPlayerData().getProfile(profile, spe).asTexts(spe);
					} catch (NbtException e) {
						// pass
					}
				}
			}
		}
	}
}

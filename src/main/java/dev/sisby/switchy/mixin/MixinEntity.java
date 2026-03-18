package dev.sisby.switchy.mixin;

import dev.sisby.switchy.duck.SwitchyPlayer;
import dev.sisby.switchy.exception.NbtException;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {
	@Inject(method = "saveWithoutId", at = @At("HEAD"), cancellable = true)
	public void applyHotSwapData(ValueOutput output, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof SwitchyPlayer sp && self instanceof ServerPlayer spe) { // mods will overwrite our hotswap data if we don't do this up here.
			CompoundTag hotSwap = sp.switchy$hotSwapData();
			if (hotSwap != null) {
				for (String name : hotSwap.keySet()) output.store(name, ExtraCodecs.NBT, hotSwap.get(name));
				sp.switchy$getPlayerData().writeNbt(spe.createCommandSourceStack().getServer().registryAccess(), output);
				ci.cancel();
			}
		}
	}

	@Inject(method = "load", at = @At("TAIL"))
	public void cacheDisplayData(ValueInput input, CallbackInfo ci) {
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

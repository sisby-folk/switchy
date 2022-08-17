package folk.sisby.switchy.mixin;

import com.mojang.authlib.GameProfile;
import folk.sisby.switchy.SwitchyPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity {

	public MixinServerPlayerEntity(World world, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(world, blockPos, f, gameProfile);
	}

	@Inject(at = @At("HEAD"), method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V")
	public void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		if (this instanceof SwitchyPlayer us && oldPlayer instanceof SwitchyPlayer them) {
			us.switchy$setPresets(them.switchy$getPresets());
		}
	}
}

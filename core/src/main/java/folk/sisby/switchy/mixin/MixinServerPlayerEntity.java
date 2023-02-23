package folk.sisby.switchy.mixin;

import com.mojang.authlib.GameProfile;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements SwitchyPlayer {

	public MixinServerPlayerEntity(World world, BlockPos blockPos, float f, GameProfile gameProfile, @Nullable PlayerPublicKey playerPublicKey) {
		super(world, blockPos, f, gameProfile, playerPublicKey);
	}

	SwitchyPresets switchy$switchyPresets;

	@SuppressWarnings("unused")
	@Inject(at = @At("TAIL"), method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void writeCustomDataToNbt(NbtCompound tag, CallbackInfo ci) {
		if (switchy$switchyPresets != null) {
			switchy$switchyPresets.saveCurrentPreset((ServerPlayerEntity) (Object) this);
			tag.put("switchy:presets", switchy$switchyPresets.toNbt());
		}
	}

	@SuppressWarnings("unused")
	@Inject(at = @At("TAIL"), method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void readCustomDataFromNbt(NbtCompound tag, CallbackInfo ci) {
		SwitchyPresets presets = new SwitchyPresets(true);
		presets.fillFromNbt(tag.getCompound("switchy:presets"));
		switchy$switchyPresets = presets;
	}

	@Inject(at = @At("HEAD"), method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V")
	public void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		if (oldPlayer instanceof SwitchyPlayer them) {
			((SwitchyPlayer) (Object) this).switchy$setPresets(them.switchy$getPresets());
		}
	}

	@Override
	public void switchy$setPresets(SwitchyPresets presets) {
		switchy$switchyPresets = presets;
	}

	@Override
	public SwitchyPresets switchy$getPresets() {
		return switchy$switchyPresets;
	}
}

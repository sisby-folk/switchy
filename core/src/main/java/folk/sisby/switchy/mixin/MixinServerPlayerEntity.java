package folk.sisby.switchy.mixin;

import com.mojang.authlib.GameProfile;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.presets.SwitchyPresetsImpl;
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

/**
 * Integrates Switchy preset data into player data.
 *
 * @author Sisby folk
 * @since 1.0.0
 */
@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements SwitchyPlayer {

	private SwitchyPresets switchy$switchyPresets;

	private MixinServerPlayerEntity(World world, BlockPos blockPos, float f, GameProfile gameProfile, @Nullable PlayerPublicKey playerPublicKey) {
		super(world, blockPos, f, gameProfile, playerPublicKey);
	}

	@SuppressWarnings({"unused", "DataFlowIssue"})
	@Inject(at = @At("TAIL"), method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	private void writeCustomDataToNbt(NbtCompound tag, CallbackInfo ci) {
		if (switchy$switchyPresets != null) {
			switchy$switchyPresets.saveCurrentPreset((ServerPlayerEntity) (Object) this);
			tag.put("switchy:presets", switchy$switchyPresets.toNbt());
		}
	}

	@SuppressWarnings("unused")
	@Inject(at = @At("TAIL"), method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	private void readCustomDataFromNbt(NbtCompound tag, CallbackInfo ci) {
		SwitchyPresets presets = new SwitchyPresetsImpl(true);
		presets.fillFromNbt(tag.getCompound("switchy:presets"));
		switchy$switchyPresets = presets;
	}

	@SuppressWarnings("RedundantCast")
	@Inject(at = @At("HEAD"), method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V")
	private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
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

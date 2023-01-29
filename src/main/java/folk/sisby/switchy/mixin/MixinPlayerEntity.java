package folk.sisby.switchy.mixin;

import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements SwitchyPlayer {

	protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	SwitchyPresets switchy$switchyPresets;

	@SuppressWarnings("unused")
	@Inject(at = @At("TAIL"), method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void writeCustomDataToNbt(NbtCompound tag, CallbackInfo ci) {
		if (switchy$switchyPresets != null) {
			switchy$switchyPresets.saveCurrentPreset((PlayerEntity) (Object) this);
			tag.put("switchy:presets", switchy$switchyPresets.toNbt());
		}
	}

	@SuppressWarnings("unused")
	@Inject(at = @At("TAIL"), method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void readCustomDataFromNbt(NbtCompound tag, CallbackInfo ci) {
		switchy$switchyPresets = SwitchyPresets.fromNbt(tag.getCompound("switchy:presets"), (PlayerEntity)(Object) this);
	}

	@Override
	public void switchy$setPresets(SwitchyPresets presets) {
		this.switchy$switchyPresets = presets;
	}

	@Override
	public SwitchyPresets switchy$getPresets() {
		return switchy$switchyPresets;
	}

}

package dev.sisby.switchy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.duck.SwitchyPlayHandler;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements SwitchyPlayer {
	private SwitchyPlayerData switchy$playerData = null;
	private SwitchyProfile switchy$sayProfile = null;
	private NbtCompound switchy$hotSwap = null;
	private NbtCompound switchy$reloadData = null;

	@Override
	public NbtCompound switchy$hotSwapData() {
		return switchy$hotSwap;
	}

	@Override
	public void switchy$startReload() {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (switchy$playerData != null) {
			switchy$reloadData = new NbtCompound();
			switchy$playerData.writeNbt(self.getServer().getRegistryManager(), switchy$reloadData);
		}
	}

	@Override
	public void switchy$finishReload() {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (switchy$reloadData != null) {
			switchy$playerData = SwitchyPlayerData.fromNbt(self.getServer().getRegistryManager(), switchy$reloadData);
			switchy$reloadData = null;
		}
	}

	@Override
	public void switchy$hotSwap(NbtCompound nbt, Text reason) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		switchy$hotSwap = nbt;
		((SwitchyPlayHandler) self.networkHandler).switchy$hotSwap();
	}

	@Override
	public SwitchyPlayerData switchy$getOrCreatePlayerData() {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (switchy$playerData == null) {
			NbtCompound nbt = new NbtCompound();
			self.writeNbt(nbt);
			switchy$playerData = SwitchyPlayerData.create(self, nbt);
			switchy$playerData.validate(self, nbt);
		}
		return switchy$playerData;
	}

	@Override
	public SwitchyPlayerData switchy$getPlayerData() {
		return switchy$playerData;
	}

	@Override
	public SwitchyProfile switchy$getSayProfile() {
		return switchy$sayProfile;
	}

	@Override
	public void switchy$setSayProfile(SwitchyProfile profile) {
		switchy$sayProfile = profile;
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void readPlayerData(NbtCompound nbt, CallbackInfo ci) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (nbt.contains(Switchy.ID)) {
			switchy$playerData = SwitchyPlayerData.fromNbt(self.getServer().getRegistryManager(), nbt);
			switchy$playerData.validate(self, nbt);
		} else if (nbt.contains("switchy:presets")) {
			switchy$playerData = SwitchyPlayerData.create(self, nbt);
			switchy$playerData.validate(self, nbt);
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void writePlayerData(NbtCompound nbt, CallbackInfo ci) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (switchy$playerData != null && switchy$reloadData == null) switchy$playerData.writeNbt(self.getServer().getRegistryManager(), nbt);
	}

	@Inject(method = "copyFrom", at = @At("TAIL"))
	public void copyPlayerData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		switchy$playerData = ((MixinServerPlayerEntity) (Object) oldPlayer).switchy$playerData;
	}

	@ModifyReturnValue(method = "acceptsMessage", at = @At("RETURN"))
	private boolean dontSendMessagesDuringHotswap(boolean original) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		return original && !((SwitchyPlayHandler) self.networkHandler).switchy$isHotSwap();
	}
}

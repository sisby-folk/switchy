package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer implements SwitchyPlayer {
	private SwitchyPlayerData switchy$playerData = null;
	private CompoundTag switchy$hotSwap = null;
	private CompoundTag switchy$reloadData = null;

	@Override
	public CompoundTag switchy$hotSwapData() {
		return switchy$hotSwap;
	}

	@Override
	public void switchy$startReload() {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$playerData != null) {
			switchy$reloadData = new CompoundTag();
			switchy$playerData.writeNbt(self.getServer().registryAccess(), switchy$reloadData);
		}
	}

	@Override
	public void switchy$finishReload() {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$reloadData != null) {
			switchy$playerData = SwitchyPlayerData.fromNbt(self.getServer().registryAccess(), switchy$reloadData);
			switchy$reloadData = null;
		}
	}

	@Override
	public void switchy$hotSwap(CompoundTag nbt, Component reason) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		switchy$hotSwap = nbt;
		self.connection.disconnect(reason);
	}

	@Override
	public SwitchyPlayerData switchy$getOrCreatePlayerData() {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$playerData == null) {
			CompoundTag nbt = new CompoundTag();
			self.saveWithoutId(nbt);
			switchy$playerData = SwitchyPlayerData.create(self, nbt);
			switchy$playerData.validate(self, nbt);
		}
		return switchy$playerData;
	}

	@Override
	public SwitchyPlayerData switchy$getPlayerData() {
		return switchy$playerData;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readPlayerData(CompoundTag nbt, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (nbt.contains(Switchy.ID)) {
			switchy$playerData = SwitchyPlayerData.fromNbt(self.getServer().registryAccess(), nbt);
			switchy$playerData.validate(self, nbt);
		} else if (nbt.contains("switchy:presets")) {
			switchy$playerData = SwitchyPlayerData.create(self, nbt);
			switchy$playerData.validate(self, nbt);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void writePlayerData(CompoundTag nbt, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$playerData != null && switchy$reloadData == null) switchy$playerData.writeNbt(self.getServer().registryAccess(), nbt);
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	public void copyPlayerData(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		switchy$playerData = ((MixinServerPlayer) (Object) oldPlayer).switchy$playerData;
	}
}

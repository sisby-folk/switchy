package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements SwitchyPlayer {
	private SwitchyPlayerData switchy$playerData = null;
	private NbtCompound switchy$hotSwap = null;
	private NbtCompound switchy$reloadData = null;

	@Override
	public NbtCompound switchy$hotSwapData() {
		return switchy$hotSwap;
	}

	@Override
	public void switchy$startReload() {
		if (switchy$playerData != null) {
			switchy$reloadData = new NbtCompound();
			switchy$playerData.writeNbt(switchy$reloadData);
		}
	}

	@Override
	public void switchy$finishReload() {
		if (switchy$reloadData != null) {
			switchy$playerData = SwitchyPlayerData.fromNbt(switchy$reloadData);
			switchy$reloadData = null;
		}
	}

	@Override
	public void switchy$hotSwap(NbtCompound nbt, Text reason) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		switchy$hotSwap = nbt;
		self.networkHandler.disconnect(reason);
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

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void readPlayerData(NbtCompound nbt, CallbackInfo ci) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (nbt.contains(Switchy.ID)) {
			switchy$playerData = SwitchyPlayerData.fromNbt(nbt);
			switchy$playerData.validate(self, nbt);
		} else if (nbt.contains("switchy:presets")) {
			switchy$playerData = SwitchyPlayerData.create(self, nbt);
			switchy$playerData.validate(self, nbt);
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void writePlayerData(NbtCompound nbt, CallbackInfo ci) {
		if (switchy$playerData != null && switchy$reloadData == null) switchy$playerData.writeNbt(nbt);
	}

	@Inject(method = "copyFrom", at = @At("TAIL"))
	public void copyPlayerData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		switchy$playerData = ((MixinServerPlayerEntity) (Object) oldPlayer).switchy$playerData;
	}
}

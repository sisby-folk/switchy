package dev.sisby.switchy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.duck.SwitchyPlayHandler;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((AccessServerPlayer) self).getServer().registryAccess());
			switchy$playerData.writeNbt(((AccessServerPlayer) self).getServer().registryAccess(), output);
			switchy$reloadData = output.buildResult();
		}
	}

	@Override
	public void switchy$finishReload() {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$reloadData != null) {
			ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, ((AccessServerPlayer) self).getServer().registryAccess(), switchy$reloadData);
			switchy$playerData = SwitchyPlayerData.fromNbt(((AccessServerPlayer) self).getServer().registryAccess(), input);
			switchy$reloadData = null;
		}
	}

	@Override
	public void switchy$hotSwap(CompoundTag nbt, Component reason) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		switchy$hotSwap = nbt;
		((SwitchyPlayHandler) self.connection).switchy$hotSwap();
	}

	@Override
	public SwitchyPlayerData switchy$getOrCreatePlayerData() {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$playerData == null) {
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((AccessServerPlayer) self).getServer().registryAccess());
			self.saveWithoutId(output);
			ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, ((AccessServerPlayer) self).getServer().registryAccess(), output.buildResult());
			switchy$playerData = SwitchyPlayerData.create(self, input);
			switchy$playerData.validate(self, input);
		}
		return switchy$playerData;
	}

	@Override
	public SwitchyPlayerData switchy$getPlayerData() {
		return switchy$playerData;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readPlayerData(ValueInput input, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (input.contains(Switchy.ID)) {
			switchy$playerData = SwitchyPlayerData.fromNbt(((AccessServerPlayer) self).getServer().registryAccess(), input);
			switchy$playerData.validate(self, input);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void writePlayerData(ValueOutput output, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (switchy$playerData != null && switchy$reloadData == null) switchy$playerData.writeNbt(((AccessServerPlayer) self).getServer().registryAccess(), output);
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	public void copyPlayerData(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		switchy$playerData = ((MixinServerPlayer) (Object) oldPlayer).switchy$playerData;
	}

	@ModifyReturnValue(method = "acceptsSystemMessages", at = @At("RETURN"))
	private boolean dontSendMessagesDuringHotswap(boolean original) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		return original && !((SwitchyPlayHandler) self.connection).switchy$isHotSwap();
	}
}

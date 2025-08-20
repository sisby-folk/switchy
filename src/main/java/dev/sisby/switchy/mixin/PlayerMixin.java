package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayerMixin implements SwitchyPlayer {
	private SwitchyPlayerData switchy$playerData = null;
	private NbtCompound switchy$hotSwap = null;

	@Override
	public void switchy$hotSwap(NbtCompound nbt, Text reason) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		switchy$hotSwap = nbt;
		ServerPlayNetworking.reconfigure(self.networkHandler);
	}

	@Override
	public SwitchyPlayerData switchy$playerData() {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (switchy$playerData == null) switchy$playerData = SwitchyPlayerData.create(self);
		return switchy$playerData;
	}

	@Inject(method = "readCustomData", at = @At("TAIL"))
	public void readPlayerData(ReadView view, CallbackInfo ci) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		switchy$playerData = view.read(Switchy.ID, SwitchyPlayerData.CODEC).orElseGet(() -> SwitchyPlayerData.create(self));
		switchy$playerData.init(self, view);
	}

	@Inject(method = "writeCustomData", at = @At("HEAD"), cancellable = true)
	public void applyHotSwapData(WriteView view, CallbackInfo ci) {
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		if (switchy$hotSwap != null && view instanceof NbtWriteView nbtView) {
			if (self.getServer().isHost(self.getGameProfile())) { // hosts don't support reconfiguration unless we patch this
				self.getServer().getSaveProperties().getPlayerData().entrySet().clear();
				self.getServer().getSaveProperties().getPlayerData().copyFrom(switchy$hotSwap);
				NbtWriteView writeView = NbtWriteView.create(new ErrorReporter.Logging(self.getErrorReporterContext(), Switchy.LOGGER), self.getRegistryManager());
				writePlayerData(writeView, ci);
				self.getServer().getSaveProperties().getPlayerData().copyFrom(writeView.getNbt());
			}
			nbtView.getNbt().copyFrom(switchy$hotSwap);
			writePlayerData(view, ci);
			ci.cancel();
		}
	}

	@Inject(method = "writeCustomData", at = @At("TAIL"))
	public void writePlayerData(WriteView view, CallbackInfo ci) {
		if (switchy$playerData != null && switchy$playerData.size() > 1) {
			view.put(Switchy.ID, SwitchyPlayerData.CODEC, switchy$playerData);
		}
	}

	@Inject(method = "copyFrom", at = @At("TAIL"))
	public void copyPlayerData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		switchy$playerData = ((PlayerMixin) (Object) oldPlayer).switchy$playerData;
	}
}

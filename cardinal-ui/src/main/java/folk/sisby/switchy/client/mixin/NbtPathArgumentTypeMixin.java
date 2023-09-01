package folk.sisby.switchy.client.mixin;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.client.api.AllValueElementNode;
import net.minecraft.command.argument.NbtPathArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtPathArgumentType.class)
public class NbtPathArgumentTypeMixin {
	@Inject(method = "parseNode", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/NbtPathArgumentType;readName(Lcom/mojang/brigadier/StringReader;)Ljava/lang/String;"), cancellable = true)
	private static void AddAllValueElementNode(StringReader reader, boolean root, CallbackInfoReturnable<NbtPathArgumentType.PathNode> cir) {
		if (reader.peek() == '*') {
			reader.skip();
			cir.setReturnValue(AllValueElementNode.INSTANCE);
			cir.cancel();
		}
	}
}

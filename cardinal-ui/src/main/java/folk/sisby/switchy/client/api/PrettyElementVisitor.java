package folk.sisby.switchy.client.api;

import folk.sisby.switchy.util.Feedback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class PrettyElementVisitor extends NbtTextFormatter implements NbtElementVisitor {
	public PrettyElementVisitor() {
		super("", 0);
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void visitString(NbtString element) {
		try {
			this.result = Text.Serializer.fromJson(element.asString()).formatted(Formatting.GREEN);
			return;
		} catch (Exception ignored) {}
		Registries.ITEM.getOrEmpty(Identifier.tryParse(element.asString())).ifPresentOrElse(
			i -> this.result = Feedback.translatable(i.getTranslationKey()).formatted(Formatting.AQUA),
			() -> this.result = Feedback.literal(element.asString()).formatted(Formatting.GREEN)
		);
	}

	@Override
	public void visitByte(NbtByte element) {
		this.result = Feedback.literal(String.valueOf(element.byteValue() == 0 ? "no" : (element.byteValue() == 1 ? "yes": element.numberValue()))).formatted(Formatting.GOLD);
	}

	@Override
	public void visitShort(NbtShort element) {
		this.result = Feedback.literal(String.valueOf(element.numberValue())).formatted(Formatting.GOLD);
	}

	@Override
	public void visitInt(NbtInt element) {
		this.result = Feedback.literal(String.valueOf(element.numberValue())).formatted(Formatting.GOLD);
	}

	@Override
	public void visitLong(NbtLong element) {
		this.result = Feedback.literal(String.valueOf(element.numberValue())).formatted(Formatting.GOLD);
	}

	@Override
	public void visitFloat(NbtFloat element) {
		this.result = Feedback.literal(String.valueOf(element.floatValue())).formatted(Formatting.GOLD);
	}

	@Override
	public void visitDouble(NbtDouble element) {
		this.result = Feedback.literal(String.valueOf(element.doubleValue())).formatted(Formatting.GOLD);
	}

	@Override
	public void visitCompound(NbtCompound compound) {
		try {
			ItemStack stack = ItemStack.fromNbt(compound);
			MutableText text = Text.empty();
			if (stack.getCount() > 1) text.append(Feedback.literal(stack.getCount() + " "));
			text.append(stack.getName());
			text.formatted(Formatting.AQUA);
			this.result = text;
			return;
		} catch (Exception ignored) {}
		super.visitCompound(compound);
	}
}

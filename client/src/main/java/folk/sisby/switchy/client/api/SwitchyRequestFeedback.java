package folk.sisby.switchy.client.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Collection;

public record SwitchyRequestFeedback(SwitchyFeedbackStatus status, Collection<MutableText> messages) {
	private static final String KEY_STATUS = "status";
	private static final String KEY_MESSAGES_LIST = "messages";

	/**
	 * Deserialize the object from NBT.
	 *
	 * @param nbt an NBT representation of the object.
	 * @return an object constructed from the NBT.
	 */
	public static SwitchyRequestFeedback fromNbt(NbtCompound nbt) {
		return new SwitchyRequestFeedback(SwitchyFeedbackStatus.valueOf(nbt.getString(KEY_STATUS)), nbt.getList(KEY_MESSAGES_LIST, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Text.Serializer::fromJson).toList());
	}

	/**
	 * Serialize the object to NBT.
	 *
	 * @return an NBT representation of the object.
	 */
	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString(KEY_STATUS, status.name());
		NbtList nbtMessages = new NbtList();
		nbtMessages.addAll(messages.stream().map(Text.Serializer::toJson).map(NbtString::of).toList());
		nbt.put(KEY_MESSAGES_LIST, nbtMessages);
		return nbt;
	}
}

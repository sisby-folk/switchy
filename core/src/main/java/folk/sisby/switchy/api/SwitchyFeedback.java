package folk.sisby.switchy.api;

import fr.catcore.server.translations.api.LocalizationTarget;
import fr.catcore.server.translations.api.text.LocalizableText;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record SwitchyFeedback(SwitchyFeedbackStatus status, Collection<Text> messages) {
	private static final String KEY_STATUS = "status";
	private static final String KEY_MESSAGES_LIST = "messages";

	/**
	 * Deserialize the object from NBT.
	 *
	 * @param nbt an NBT representation of the object.
	 * @return an object constructed from the NBT.
	 */
	public static SwitchyFeedback fromNbt(NbtCompound nbt) {
		List<Text> msgs = new ArrayList<>();
		nbt.getList(KEY_MESSAGES_LIST, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Text.Serializer::fromJson).forEach(msgs::add);
		return new SwitchyFeedback(SwitchyFeedbackStatus.valueOf(nbt.getString(KEY_STATUS)), msgs);
	}

	/**
	 * Serialize the object to NBT.
	 *
	 * @return an NBT representation of the object.
	 */
	public NbtCompound toNbt(@Nullable ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString(KEY_STATUS, status.name());
		NbtList nbtMessages = new NbtList();
		nbtMessages.addAll(messages.stream().map(text -> Text.Serializer.toJson(player == null ? text : LocalizableText.asLocalizedFor(text, (LocalizationTarget) player))).map(NbtString::of).toList());
		nbt.put(KEY_MESSAGES_LIST, nbtMessages);
		return nbt;
	}
}

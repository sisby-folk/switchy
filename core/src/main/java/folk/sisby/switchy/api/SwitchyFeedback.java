package folk.sisby.switchy.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.server.translations.api.Localization;
import xyz.nucleoid.server.translations.api.LocalizationTarget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A feedback object representing the outcome of an action.
 *
 * @param status   the completion status of the action.
 * @param messages the feedback messages reported by the action.
 * @author Sisby folk
 * @since 2.0.0
 */
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
		nbt.getList(KEY_MESSAGES_LIST, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Text.Serialization::fromJson).forEach(msgs::add);
		return new SwitchyFeedback(SwitchyFeedbackStatus.valueOf(nbt.getString(KEY_STATUS)), msgs);
	}

	/**
	 * Serialize the object to NBT.
	 *
	 * @param player the client player being sent the NBT, if applicable.
	 * @return an NBT representation of the object.
	 */
	public NbtCompound toNbt(@Nullable ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString(KEY_STATUS, status.name());
		NbtList nbtMessages = new NbtList();
		nbtMessages.addAll(messages.stream().map(text -> Text.Serialization.toJsonString(player == null ? text : Localization.text(text, LocalizationTarget.of(player).getLanguage(), true))).map(NbtString::of).toList());
		nbt.put(KEY_MESSAGES_LIST, nbtMessages);
		return nbt;
	}
}

package folk.sisby.switchy.api.module;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;

import static folk.sisby.switchy.util.Feedback.translatable;


/**
 * Static settings for a module, set during registration.
 *
 * @param isDefault         whether the module should be enabled for new players.
 * @param editable          permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
 * @param applyDependencies which other modules need to be applied to the player before this one.
 * @param uniqueIds         a collection of unique IDs that cannot collide with other added modules.
 * @param deletionWarning   a text warning explaining what data will be lost if the module is disabled.
 * @see SwitchyModuleRegistry
 */
public record SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable,
								Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds,
								MutableText deletionWarning) {
	private static final String KEY_DEFAULT_DISABLE = "commands.switchy.module.disable.warn";

	private static final String KEY_DEFAULT = "default";
	private static final String KEY_EDITABLE = "editable";
	private static final String KEY_APPLY_DEPENDENCIES = "applyDependencies";
	private static final String KEY_UNIQUE_IDS = "uniqueIds";
	private static final String KEY_DELETION_WARNING = "deletionWarning";

	/**
	 * @param isDefault whether the module should be enabled for new players.
	 * @param editable  permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable) {
		this(isDefault, editable, Set.of(), Set.of(), translatable(KEY_DEFAULT_DISABLE));
	}

	/**
	 * @param isDefault         whether the module should be enabled for new players.
	 * @param editable          permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 * @param applyDependencies which other modules need to be applied to the player before this one.
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies) {
		this(isDefault, editable, applyDependencies, Set.of(), translatable(KEY_DEFAULT_DISABLE));
	}

	/**
	 * @param isDefault         whether the module should be enabled for new players.
	 * @param editable          permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 * @param applyDependencies which other modules need to be applied to the player before this one.
	 * @param uniqueIds         a collection of unique IDs that cannot collide with other added modules.
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds) {
		this(isDefault, editable, applyDependencies, uniqueIds, translatable(KEY_DEFAULT_DISABLE));
	}

	/**
	 * @param isDefault         whether the module should be enabled for new players.
	 * @param editable          permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 * @param applyDependencies which other modules need to be applied to the player before this one.
	 * @param deletionWarning   a text warning to show when the user attempts to disable the module.
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, MutableText deletionWarning) {
		this(isDefault, editable, applyDependencies, Set.of(), deletionWarning);
	}

	/**
	 * Deserialize the instance from NBT.
	 *
	 * @param nbt an NBT representation of the instance.
	 * @return an instance constructed from the NBT.
	 */
	public static SwitchyModuleInfo fromNbt(NbtCompound nbt) {
		return new SwitchyModuleInfo(
				nbt.getBoolean(KEY_DEFAULT),
				SwitchyModuleEditable.valueOf(nbt.getString(KEY_EDITABLE)),
				nbt.getList(KEY_APPLY_DEPENDENCIES, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).toList(),
				nbt.getList(KEY_UNIQUE_IDS, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).toList(),
				MutableText.Serializer.fromJson(nbt.getString(KEY_DELETION_WARNING))
		);
	}

	/**
	 * Serialize the instance to NBT.
	 *
	 * @return an NBT representation of the instance.
	 */
	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putBoolean(KEY_DEFAULT, isDefault);
		nbt.putString(KEY_EDITABLE, editable.name());
		NbtList nbtDependencies = new NbtList();
		nbtDependencies.addAll(applyDependencies.stream().map(Identifier::toString).map(NbtString::of).toList());
		nbt.put(KEY_APPLY_DEPENDENCIES, nbtDependencies);
		NbtList nbtUniqueIds = new NbtList();
		nbtUniqueIds.addAll(uniqueIds.stream().map(Identifier::toString).map(NbtString::of).toList());
		nbt.put(KEY_UNIQUE_IDS, nbtUniqueIds);
		nbt.putString(KEY_DELETION_WARNING, Text.Serializer.toJson(deletionWarning));
		return nbt;
	}
}

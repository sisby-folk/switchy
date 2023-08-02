package folk.sisby.switchy.api.module;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.api.SwitchySerializable;
import fr.catcore.server.translations.api.LocalizationTarget;
import fr.catcore.server.translations.api.text.LocalizableText;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static folk.sisby.switchy.util.Feedback.translatable;


/**
 * Static withtings for a module, with during registration.
 *
 * @see SwitchyModuleRegistry
 */
public final class SwitchyModuleInfo {
	private static final String KEY_DEFAULT = "default";
	private static final String KEY_EDITABLE = "editable";
	private static final String KEY_DESCRIPTION = "description";
	private static final String KEY_WHEN_ENABLED = "descriptionWhenEnabled";
	private static final String KEY_WHEN_DISABLED = "descriptionWhenDisabled";
	private static final String KEY_APPLY_DEPENDENCIES = "applyDependencies";
	private static final String KEY_UNIQUE_IDS = "uniqueIds";
	private static final String KEY_DELETION_WARNING = "deletionWarning";

	private boolean isDefault;
	private SwitchyModuleEditable editable;
	private MutableText description;
	private MutableText deletionWarning = translatable("commands.switchy.module.help.disable.warn.default");
	private MutableText descriptionWhenEnabled = translatable("commands.switchy.module.help.enabled.default");
	private MutableText descriptionWhenDisabled = translatable("commands.switchy.module.help.disabled.default");
	private Set<Identifier> applyDependencies = new HashSet<>();
	private Set<Identifier> uniqueIds = new HashSet<>();
	private Consumer<LiteralArgumentBuilder<ServerCommandSource>> configCommands = null;
	private Supplier<SwitchySerializable> moduleConfig = null;

	/**
	 * Constructs an instance with the minimum options.
	 * To change other defaulted options, use the various chaining {@code .with()} methods.
	 *
	 * @param isDefault   whether the module should be enabled for new players.
	 *                    Usually {@code false}.
	 * @param editable    permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 *                    Usually {@code OPERATOR}.
	 * @param description a brief text description of the module. See {@link SwitchyModuleInfo#withDescriptionWhenDisabled(MutableText)} and {@link SwitchyModuleInfo#withDescriptionWhenEnabled(MutableText)}.
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, MutableText description) {
		this.isDefault = isDefault;
		this.editable = editable;
		this.description = description;
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
				MutableText.Serializer.fromJson(nbt.getString(KEY_DESCRIPTION))
		)
				.withDescriptionWhenEnabled(MutableText.Serializer.fromJson(nbt.getString(KEY_WHEN_ENABLED)))
				.withDescriptionWhenDisabled(MutableText.Serializer.fromJson(nbt.getString(KEY_WHEN_DISABLED)))
				.withApplyDependencies(nbt.getList(KEY_APPLY_DEPENDENCIES, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).collect(Collectors.toSet()))
				.withUniqueIds(nbt.getList(KEY_UNIQUE_IDS, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).collect(Collectors.toSet()))
				.withDeletionWarning(MutableText.Serializer.fromJson(nbt.getString(KEY_DELETION_WARNING)));
	}

	/**
	 * Serialize the instance to NBT.
	 *
	 * @param player the client player being sent the NBT, if applicable.
	 * @return an NBT representation of the instance.
	 */
	public NbtCompound toNbt(@Nullable ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		nbt.putBoolean(KEY_DEFAULT, isDefault);
		nbt.putString(KEY_EDITABLE, editable.name());
		nbt.putString(KEY_DESCRIPTION, Text.Serializer.toJson(player == null ? description : LocalizableText.asLocalizedFor(description, (LocalizationTarget) player)));
		nbt.putString(KEY_WHEN_ENABLED, Text.Serializer.toJson(player == null ? descriptionWhenEnabled : LocalizableText.asLocalizedFor(descriptionWhenEnabled, (LocalizationTarget) player)));
		nbt.putString(KEY_WHEN_DISABLED, Text.Serializer.toJson(player == null ? descriptionWhenDisabled : LocalizableText.asLocalizedFor(descriptionWhenDisabled, (LocalizationTarget) player)));
		nbt.putString(KEY_DELETION_WARNING, Text.Serializer.toJson(player == null ? deletionWarning : LocalizableText.asLocalizedFor(deletionWarning, (LocalizationTarget) player)));
		NbtList nbtDependencies = new NbtList();
		nbtDependencies.addAll(applyDependencies.stream().map(Identifier::toString).map(NbtString::of).toList());
		nbt.put(KEY_APPLY_DEPENDENCIES, nbtDependencies);
		NbtList nbtUniqueIds = new NbtList();
		nbtUniqueIds.addAll(uniqueIds.stream().map(Identifier::toString).map(NbtString::of).toList());
		nbt.put(KEY_UNIQUE_IDS, nbtUniqueIds);
		return nbt;
	}

	/**
	 * Sets the default status of the module.
	 *
	 * @param isDefault whether the module should be enabled for new players.
	 * @return this.
	 */
	public SwitchyModuleInfo withDefault(boolean isDefault) {
		this.isDefault = isDefault;
		return this;
	}

	/**
	 * Sets the cold-editing permissions for the module.
	 *
	 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 * @return this.
	 */
	public SwitchyModuleInfo withEditable(SwitchyModuleEditable editable) {
		this.editable = editable;
		return this;
	}

	/**
	 * Sets the description for the module.
	 *
	 * @param description a brief text description of the module. See {@link SwitchyModuleInfo#withDescriptionWhenDisabled(MutableText)} and {@link SwitchyModuleInfo#withDescriptionWhenEnabled(MutableText)}.
	 * @return this.
	 */
	public SwitchyModuleInfo withDescription(MutableText description) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the deletion warning for the module.
	 *
	 * @param deletionWarning a text warning explaining what data will be lost if the module is disabled.
	 *                        Often prepended with "Warning: ".
	 *                        Defaults to "Module data will be deleted for all presets".
	 * @return this.
	 */
	public SwitchyModuleInfo withDeletionWarning(MutableText deletionWarning) {
		this.deletionWarning = deletionWarning;
		return this;
	}

	/**
	 * Sets the "when enabled" description of the module.
	 * Explains the purpose/functionality of the module being enabled.
	 * E.g. "Players have separate inventories per preset".
	 *
	 * @param descriptionWhenEnabled the "when enabled" description text.
	 * @return this.
	 */
	public SwitchyModuleInfo withDescriptionWhenEnabled(MutableText descriptionWhenEnabled) {
		this.descriptionWhenEnabled = descriptionWhenEnabled;
		return this;
	}

	/**
	 * Sets the "when disabled" description of the module.
	 * Explains the purpose/functionality of the module being disabled.
	 * E.g. "Players have one shared inventory across all presets".
	 *
	 * @param descriptionWhenDisabled the "when disabled" description text.
	 * @return this.
	 */
	public SwitchyModuleInfo withDescriptionWhenDisabled(MutableText descriptionWhenDisabled) {
		this.descriptionWhenDisabled = descriptionWhenDisabled;
		return this;
	}

	/**
	 * Sets which other modules need to be applied to the player before this one.
	 *
	 * @param applyDependencies Identifiers for other modules that must be applied first.
	 * @return this.
	 */
	public SwitchyModuleInfo withApplyDependencies(Set<Identifier> applyDependencies) {
		this.applyDependencies = applyDependencies;
		return this;
	}

	/**
	 * Sets arbitrary IDs that must not clash with any other module.
	 *
	 * @param uniqueIds a set of unique IDs that cannot collide with other added modules.
	 * @return this.
	 */
	public SwitchyModuleInfo withUniqueIds(Set<Identifier> uniqueIds) {
		this.uniqueIds = uniqueIds;
		return this;
	}

	/**
	 * Sets a configuration object that can be used to store player-level data for the module, like settings.
	 *
	 * @param moduleConfig a supplier for a player-scoped configuration object
	 * @return this.
	 */
	public SwitchyModuleInfo withModuleConfig(Supplier<SwitchySerializable> moduleConfig) {
		this.moduleConfig = moduleConfig;
		return this;
	}

	/**
	 * Sets the callback used to generate commands under /switchy module config [id].
	 *
	 * @param configCommands a consumer for the argument builder of the [id] arg in the command.
	 * @return this.
	 */
	public SwitchyModuleInfo withConfigCommands(Consumer<LiteralArgumentBuilder<ServerCommandSource>> configCommands) {
		this.configCommands = configCommands;
		return this;
	}

	/**
	 * Gets the default status of the module.
	 *
	 * @return true if the module should be enabled for new players, otherwise false.
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Gets the cold-editing permissions for the module.
	 *
	 * @return permissions for cold-editing the module, see {@link SwitchyModuleEditable}.
	 */
	public SwitchyModuleEditable editable() {
		return editable;
	}

	/**
	 * Gets the description for the module.
	 *
	 * @return a brief text description of the module. See {@link SwitchyModuleInfo#withDescriptionWhenDisabled(MutableText)} and {@link SwitchyModuleInfo#withDescriptionWhenEnabled(MutableText)}.
	 */
	public MutableText description() {
		return description;
	}

	/**
	 * Gets the deletion warning for the module.
	 *
	 * @return a text warning explaining what data will be lost if the module is disabled.
	 */
	public MutableText deletionWarning() {
		return deletionWarning;
	}

	/**
	 * Gets which other modules need to be applied to the player before this one.
	 *
	 * @return identifiers for other modules that must be applied first.
	 */
	public Set<Identifier> applyDependencies() {
		return applyDependencies;
	}

	/**
	 * Gets arbitrary IDs that must not clash with any other module.
	 *
	 * @return a set of unique IDs that cannot collide with other added modules.
	 */
	public Set<Identifier> uniqueIds() {
		return uniqueIds;
	}

	/**
	 * Gets the "when enabled" description of the module.
	 * Explains the purpose/functionality of the module being enabled.
	 * E.g. "Players have separate inventories per preset".
	 *
	 * @return the "when enabled" description text.
	 */
	public MutableText descriptionWhenEnabled() {
		return descriptionWhenEnabled;
	}

	/**
	 * Gets the "when disabled" description of the module.
	 * Explains the purpose/functionality of the module being disabled.
	 * E.g. "Players have one shared inventory across all presets".
	 *
	 * @return the "when disabled" description text.
	 */
	public MutableText descriptionWhenDisabled() {
		return descriptionWhenDisabled;
	}

	/**
	 * Gets the configuration object that can be used to store player-level data for the module, like settings.
	 * @return a supplier for a player-scoped configuration object
	 */
	public Supplier<SwitchySerializable> moduleConfig() {
		return moduleConfig;
	}

	/**
	 * Adds any registered config commands to the argument builder specified.
	 *
	 * @param configIdArgument the module ID-like literal argument where config arguments will be added.
	 * @return whether any commands were added.
	 */
	public boolean configCommands(LiteralArgumentBuilder<ServerCommandSource> configIdArgument) {
		if (configCommands != null) {
			configCommands.accept(configIdArgument);
			return !configIdArgument.getArguments().isEmpty();
		}
		return false;
	}
}

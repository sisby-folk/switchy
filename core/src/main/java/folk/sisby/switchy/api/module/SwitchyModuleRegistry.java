package folk.sisby.switchy.api.module;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.SwitchySerializable;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.util.SwitchyCommand;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provides access to module registration for addons, and exposes information about the current state of the registry.
 * Effectively Static.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 2.0.0
 */
public class SwitchyModuleRegistry {
	private static final List<SwitchyModuleEditable> EDITABLE_CONFIGURABLE = List.of(SwitchyModuleEditable.ALLOWED, SwitchyModuleEditable.OPERATOR);

	private static final Map<Identifier, Supplier<SwitchyModule>> SUPPLIERS = new HashMap<>();
	private static final Map<Identifier, SwitchyModuleInfo> INFO = new HashMap<>();

	/**
	 * Allows addons to register {@link SwitchyModule} implementations to be used by Switchy.
	 *
	 * @param id                A unique identifier to associate with the module being registered.
	 * @param moduleConstructor Usually {@code ModuleName::new} - this will be called on player join.
	 * @param moduleInfo        The static settings for the module. See {@link SwitchyModuleInfo}.
	 * @throws IllegalArgumentException when {@code id} is already associated with a registered module.
	 * @throws IllegalStateException    when a {@code uniqueId} provided in {@link SwitchyModuleInfo} collides with one already registered.
	 */
	public static void registerModule(Identifier id, Supplier<SwitchyModule> moduleConstructor, SwitchyModuleInfo moduleInfo) throws IllegalArgumentException, IllegalStateException {
		if (SUPPLIERS.containsKey(id)) {
			throw new IllegalArgumentException("Specified module id is already registered");
		}
		if (INFO.values().stream().anyMatch(info -> info.uniqueIds().stream().anyMatch(moduleInfo.uniqueIds()::contains))) {
			throw new IllegalStateException("Specified uniqueId is already registered");
		}

		INFO.put(id, moduleInfo);
		SUPPLIERS.put(id, moduleConstructor);

		if (EDITABLE_CONFIGURABLE.contains(moduleInfo.editable())) {
			SwitchyModuleEditable configEditable = Switchy.CONFIG.moduleEditable.get(id.toString());
			if (configEditable == null || !EDITABLE_CONFIGURABLE.contains(configEditable)) { // Reset to default
				Switchy.CONFIG.moduleEditable.put(id.toString(), moduleInfo.editable());
			}
		} else {
			Switchy.CONFIG.moduleEditableReadOnly.put(id.toString(), moduleInfo.editable());
		}
		Switchy.LOGGER.info("[Switchy] Registered module " + id);
	}

	/**
	 * Gets the IDs of all registered modules.
	 *
	 * @return a collection of registered module identifiers.
	 */
	public static Collection<Identifier> getModules() {
		return INFO.keySet();
	}

	/**
	 * Gets an instance of a module using a registered supplier.
	 *
	 * @param id a module identifier.
	 * @return an instance of the module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @see SwitchyModule
	 */
	@ApiStatus.Internal
	public static SwitchyModule supplyModule(Identifier id) throws ModuleNotFoundException {
		if (!SUPPLIERS.containsKey(id)) throw new ModuleNotFoundException();
		return SUPPLIERS.get(id).get();
	}

	/**
	 * Whether the specified module is registered.
	 *
	 * @param id a module identifier.
	 * @return true if the module is registered, false otherwise.
	 */
	public static boolean containsModule(Identifier id) {
		return INFO.containsKey(id);
	}

	/**
	 * Whether a module should be enabled by default.
	 *
	 * @param id a module identifier.
	 * @return true if the module is default, false otherwise.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	public static boolean isDefault(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).isDefault();
	}

	/**
	 * Gets the cold-editing permissions for the specified module.
	 * Affected by server configuration.
	 *
	 * @param id a module identifier.
	 * @return the editable value registered to the specified module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 * @see SwitchyModuleEditable
	 */
	public static SwitchyModuleEditable getEditable(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		SwitchyModuleEditable baseEditable = INFO.get(id).editable();
		return EDITABLE_CONFIGURABLE.contains(baseEditable) ? Switchy.CONFIG.moduleEditable.get(id.toString()) : baseEditable;
	}

	/**
	 * Gets the description for the module.
	 *
	 * @param id a module identifier.
	 * @return a brief text description of the module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	public static MutableText getDescription(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).description();
	}

	/**
	 * Gets the "when enabled" description of a module.
	 *
	 * @param id a module identifier.
	 * @return the "when enabled" description text.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	public static MutableText getDescriptionWhenEnabled(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).descriptionWhenEnabled();
	}

	/**
	 * Gets the "when disabled" description of a module.
	 *
	 * @param id a module identifier.
	 * @return the "when disabled" description text.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	public static MutableText getDescriptionWhenDisabled(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).descriptionWhenDisabled();
	}

	/**
	 * Gets warning message that should be displayed when disabling a module.
	 *
	 * @param id a module identifier.
	 * @return the deletion warning for the module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	public static MutableText getDeletionWarning(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).deletionWarning();
	}

	/**
	 * Gets modules that must be applied to the player before the specified one during a switch.
	 *
	 * @param id a module identifier.
	 * @return collection of module apply dependency IDs for the module.
	 * @throws ModuleNotFoundException when a module with the specified ID doesn't exist.
	 */
	public static Collection<Identifier> getApplyDependencies(Identifier id) throws ModuleNotFoundException {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).applyDependencies();
	}

	/**
	 * Gets a map representation of whether a module should be enabled by default for all modules.
	 *
	 * @return a map representing which modules are enabled by default.
	 */
	public static Map<Identifier, Boolean> getModuleDefaults() {
		Map<Identifier, Boolean> outMap = new HashMap<>();
		SUPPLIERS.forEach((id, supplier) -> {
			SwitchyModule module = supplier.get();
			if (module != null) {
				outMap.put(id, isDefault(id));
			}
		});
		return outMap;
	}

	/**
	 * Generates a player-level module configuration object, if the module has one.
	 * @param id a module identifier.
	 * @return a configuration object for that module, or null if it doesn't have one.
	 */
	public static @Nullable SwitchySerializable generateModuleConfig(Identifier id) {
		if (!INFO.containsKey(id)) throw new ModuleNotFoundException();
		return INFO.get(id).moduleConfig() != null ? INFO.get(id).moduleConfig().get() : null;
	}

	/**
	 * Adds all registered config commands under literal arguments matching their module ID.
	 *
	 * @param configArgument the literal argument where module ID arguments will be added.
	 * @return whether any module ID arguments were added.
	 */
	public static boolean addConfigCommands(LiteralArgumentBuilder<ServerCommandSource> configArgument) {
		INFO.forEach((id, info) -> {
			LiteralArgumentBuilder<ServerCommandSource> idArgument = LiteralArgumentBuilder.literal(id.toString());
			if (info.configCommands(idArgument)) {
				idArgument.requires(src -> SwitchyCommand.serverPlayerOrNull(src) instanceof SwitchyPlayer sp && sp.switchy$getPresets().isModuleEnabled(id));
				configArgument.then(idArgument);
			}
		});
		return !configArgument.getArguments().isEmpty();
	}

	/**
	 * Deserialize the module info from NBT.
	 *
	 * @param nbt an NBT representation of the module info.
	 * @return an instance constructed from the NBT.
	 */
	public static Map<Identifier, SwitchyModuleInfo> infoFromNbt(NbtCompound nbt) {
		Map<Identifier, SwitchyModuleInfo> outMap = new HashMap<>();
		nbt.getKeys().forEach(key -> outMap.put(Identifier.tryParse(key), SwitchyModuleInfo.fromNbt(nbt.getCompound(key))));
		return outMap;
	}

	/**
	 * Serialize the module info to NBT.
	 *
	 * @param player the client player being sent the NBT, if applicable.
	 * @return an NBT representation of the module info.
	 */
	public static NbtCompound infoToNbt(@Nullable ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		INFO.forEach((id, info) -> {
			SwitchyModuleInfo copy = SwitchyModuleInfo.fromNbt(info.toNbt(null));
			copy.withEditable(getEditable(id));
			nbt.put(id.toString(), copy.toNbt(player));
		});
		return nbt;
	}
}

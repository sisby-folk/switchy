package folk.sisby.switchy.api;

import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.exception.ModuleNotFoundException;
import folk.sisby.switchy.api.exception.PresetNotFoundException;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static folk.sisby.switchy.SwitchyCommands.HISTORY;
import static folk.sisby.switchy.util.Feedback.*;

/**
 * API for interacting with player presets.
 * Intended for methods that directly provide feedback to the player, e.g. commands.
 * For programmatic integration with action outcomes, use {@link SwitchyPresets}.
 *
 * @author Sisby folk
 * @see SwitchyPresets
 * @since 2.0.0
 */
public class SwitchyApi {
	/**
	 * Map for help text to display in {@code /switchy help}.
	 * Addons should use {@link SwitchyEvents#COMMAND_INIT} to add to this.
	 */
	@ApiStatus.Internal
	public static final Map<Text, Predicate<ServerPlayerEntity>> HELP_TEXT = new HashMap<>();

	/**
	 * Provide help text based for the mod and addons.
	 *
	 * @param player   the relevant player.
	 * @param feedback a consumer for text feedback.
	 * @return The completion status of the action.
	 */
	public static SwitchyFeedbackStatus displayHelp(ServerPlayerEntity player, Consumer<Text> feedback) {
		HELP_TEXT.forEach((t, p) -> {
			if (p.test(player)) feedback.accept(t);
		});
		return SwitchyFeedbackStatus.SUCCESS;
	}

	/**
	 * Provide a textual representation of presets, enabled modules, and the current preset.
	 *
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @return The completion status of the action.
	 */
	public static SwitchyFeedbackStatus listPresets(SwitchyPresets presets, Consumer<Text> feedback) {
		feedback.accept(info("commands.switchy.list.presets", literal(presets.toString())));
		feedback.accept(info("commands.switchy.list.modules", presets.getEnabledModuleText()));
		feedback.accept(info("commands.switchy.list.current", literal(presets.getCurrentPreset().toString())));
		return SwitchyFeedbackStatus.SUCCESS;
	}

	/**
	 * Provide module help text, including description, and enabled/disabled effects.
	 *
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param id       a module identifier.
	 * @return The completion status of the action.
	 */
	public static SwitchyFeedbackStatus displayModuleHelp(SwitchyPresets presets, Consumer<Text> feedback, Identifier id) {
		try {
			feedback.accept(info("commands.switchy.module.help.description", literal(id.getPath()), literal(presets.isModuleEnabled(id) ? "enabled" : "disabled"), SwitchyModuleRegistry.getDescription(id)));
			feedback.accept(translatableWithArgs("commands.switchy.module.help.enabled", presets.isModuleEnabled(id) ? FORMAT_SUCCESS : FORMAT_INFO, SwitchyModuleRegistry.getDescriptionWhenEnabled(id)));
			feedback.accept(translatableWithArgs("commands.switchy.module.help.disabled", presets.isModuleEnabled(id) ? FORMAT_INFO : FORMAT_SUCCESS, SwitchyModuleRegistry.getDescriptionWhenDisabled(id)));
			return SwitchyFeedbackStatus.SUCCESS;
		} catch (ModuleNotFoundException ignored) {
			invalid("commands.switchy.module.help.fail.missing", literal(id.toString()));
			return SwitchyFeedbackStatus.INVALID;
		}
	}

	/**
	 * Creates a new preset.
	 * Provides feedback to the player based on success.
	 *
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param name     the case-insensitive name of a preset.
	 * @return The completion status of the action.
	 * @see SwitchyPresets#newPreset(String)
	 */
	public static SwitchyFeedbackStatus newPreset(SwitchyPresets presets, Consumer<Text> feedback, String name) {
		try {
			SwitchyPreset newPreset = presets.newPreset(name);
			feedback.accept(success("commands.switchy.new.success", literal(newPreset.getName())));
			return SwitchyFeedbackStatus.SUCCESS;
		} catch (InvalidWordException ignored) {
			feedback.accept(invalid("commands.switchy.new.fail.invalid"));
			return SwitchyFeedbackStatus.INVALID;
		} catch (IllegalStateException ignoredPresetExists) {
			feedback.accept(invalidTry("commands.switchy.new.fail.exists", "commands.switchy.set.command", literal(name)));
			return SwitchyFeedbackStatus.INVALID;
		}
	}

	/**
	 * Switches to the specified preset.
	 * Provides feedback to the player based on success.
	 *
	 * @param player   the relevant player.
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param name     the case-insensitive name of a preset.
	 * @return The completion status of the action.
	 * @see SwitchyPresets#switchCurrentPreset(ServerPlayerEntity, String)
	 */
	public static SwitchyFeedbackStatus switchPreset(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback, String name) {
		String oldName = presets.getCurrentPreset().toString();
		try {
			String newName = presets.switchCurrentPreset(player, name);
			feedback.accept(success("commands.switchy.set.success", literal(oldName), literal(newName)));
			return SwitchyFeedbackStatus.SUCCESS;
		} catch (PresetNotFoundException ignored) {
			feedback.accept(invalidTry("commands.switchy.set.fail.missing", "commands.switchy.list.command"));
			return SwitchyFeedbackStatus.INVALID;
		} catch (IllegalStateException ignoredPresetCurrent) {
			feedback.accept(invalidTry("commands.switchy.set.fail.current", "commands.switchy.list.command"));
			return SwitchyFeedbackStatus.INVALID;
		}
	}

	/**
	 * Switches to the specified preset.
	 * Provides feedback to the player based on success.
	 *
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param name     the case-insensitive name of a preset.
	 * @param newName  the new name for the specified preset. a single word matching {@code azAZ09_-.+}.
	 * @return The completion status of the action.
	 * @see SwitchyPresets#renamePreset(String, String)
	 */
	public static SwitchyFeedbackStatus renamePreset(SwitchyPresets presets, Consumer<Text> feedback, String name, String newName) {
		try {
			presets.renamePreset(name, newName);
			feedback.accept(success("commands.switchy.rename.success", literal(name), literal(newName)));
			return SwitchyFeedbackStatus.SUCCESS;
		} catch (InvalidWordException ignored) {
			feedback.accept(invalid("commands.switchy.rename.fail.invalid"));
			return SwitchyFeedbackStatus.INVALID;
		} catch (PresetNotFoundException ignored) {
			feedback.accept(invalidTry("commands.switchy.rename.fail.missing", "commands.switchy.list.command"));
			return SwitchyFeedbackStatus.INVALID;
		} catch (IllegalStateException ignoredPresetExists) {
			feedback.accept(invalidTry("commands.switchy.rename.fail.exists", "commands.switchy.list.command"));
			return SwitchyFeedbackStatus.INVALID;
		}
	}

	/**
	 * Deletes the specified preset. Lossy.
	 * Provides feedback to the player based on success.
	 *
	 * @param player   the relevant player.
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param name     the case-insensitive name of a preset.
	 * @return The completion status of the action.
	 * @see SwitchyPresets#deletePreset(String)
	 */
	public static SwitchyFeedbackStatus deletePreset(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback, String name) {
		try {
			presets.deletePreset(player, name, true);
		} catch (PresetNotFoundException ignored) {
			feedback.accept(invalidTry("commands.switchy.delete.fail.missing", "commands.switchy.list.command"));
			return SwitchyFeedbackStatus.INVALID;
		} catch (IllegalStateException ignoredPresetCurrent) {
			feedback.accept(invalidTry("commands.switchy.delete.fail.current", "commands.switchy.rename.command", literal(""), literal("")));
			return SwitchyFeedbackStatus.INVALID;
		}

		if (!HISTORY.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy delete " + name))) {
			feedback.accept(warn("commands.switchy.delete.warn"));
			feedback.accept(warn("commands.switchy.list.modules", presets.getEnabledModuleText()));
			feedback.accept(invalidTry("commands.switchy.delete.confirmation", "commands.switchy.delete.command", literal(name)));
			return SwitchyFeedbackStatus.CONFIRM;
		} else {
			presets.deletePreset(player, name);
			feedback.accept(success("commands.switchy.delete.success", literal(name)));
			return SwitchyFeedbackStatus.SUCCESS;
		}
	}

	/**
	 * Disables the specified module. Lossy.
	 * Provides feedback to the player based on success.
	 *
	 * @param player   the relevant player.
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param id       a module identifier.
	 * @return The completion status of the action.
	 * @see SwitchyPresets#disableModule(Identifier)
	 */
	public static SwitchyFeedbackStatus disableModule(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback, Identifier id) {
		try {
			presets.disableModule(player, id, true);
		} catch (ModuleNotFoundException ignored) {
			feedback.accept(invalid("commands.switchy.module.disable.fail.missing", literal(id.toString())));
			return SwitchyFeedbackStatus.INVALID;
		} catch (IllegalStateException ignoredModuleDisabled) {
			feedback.accept(invalid("commands.switchy.module.disable.fail.disabled", literal(id.getPath())));
			return SwitchyFeedbackStatus.INVALID;
		}

		if (!HISTORY.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy module disable " + id))) {
			feedback.accept(warn("commands.switchy.module.disable.warn", SwitchyModuleRegistry.getDeletionWarning(id)));
			feedback.accept(invalidTry("commands.switchy.module.disable.confirmation", "commands.switchy.module.disable.command", literal(id.toString())));
			return SwitchyFeedbackStatus.CONFIRM;
		} else {
			presets.disableModule(player, id);
			feedback.accept(success("commands.switchy.module.disable.success", literal(id.getPath())));
			return SwitchyFeedbackStatus.SUCCESS;
		}
	}

	/**
	 * Enables the specified module.
	 * Provides feedback to the player based on success.
	 *
	 * @param player   the relevant player.
	 * @param presets  the player's presets object.
	 * @param feedback a consumer for text feedback.
	 * @param id       a module identifier.
	 * @return The completion status of the action.
	 * @see SwitchyPresets#enableModule(Identifier)
	 */
	public static SwitchyFeedbackStatus enableModule(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback, Identifier id) {
		try {
			presets.enableModule(player, id);
			feedback.accept(success("commands.switchy.module.enable.success", literal(id.getPath())));
			return SwitchyFeedbackStatus.SUCCESS;
		} catch (ModuleNotFoundException ignored) {
			feedback.accept(invalid("commands.switchy.module.enable.fail.missing", literal(id.toString())));
			return SwitchyFeedbackStatus.INVALID;
		} catch (IllegalStateException ignoredModuleEnabled) {
			feedback.accept(invalid("commands.switchy.module.enable.fail.enabled", literal(id.getPath())));
			return SwitchyFeedbackStatus.INVALID;
		}
	}

	/**
	 * Imports presets, providing feedback for confirmation if this is the first time.
	 *
	 * @param player          The player to show confirmation and import presets to.
	 * @param feedback        a consumer for text feedback.
	 * @param importedPresets The presets to be imported.
	 * @param modules         The modules to be imported.
	 * @param command         The command to use for repeat-style confirmation.
	 * @return The completion status of the action.
	 */
	public static SwitchyFeedbackStatus confirmAndImportPresets(ServerPlayerEntity player, Map<String, SwitchyPreset> importedPresets, List<Identifier> modules, String command, Consumer<Text> feedback) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

		// Print info and stop if confirmation is required.
		if (!HISTORY.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command(command))) {
			feedback.accept(warn("commands.switchy.import.warn.info", literal(String.valueOf(importedPresets.size())), literal(String.valueOf(modules.size()))));
			feedback.accept(warn("commands.switchy.list.presets", getHighlightedListText(importedPresets.keySet().stream().sorted().toList(), List.of(new Pair<>(presets.getPresetNames()::contains, Formatting.DARK_RED)))));
			feedback.accept(warn("commands.switchy.import.warn.collision"));
			feedback.accept(warn("commands.switchy.list.modules", getIdListText(modules)));
			feedback.accept(invalid("commands.switchy.import.confirmation", literal("/" + command)));
			HISTORY.put(player.getUuid(), command(command));
			return SwitchyFeedbackStatus.CONFIRM;
		}

		// Import
		presets.importFromOther(player, importedPresets);
		feedback.accept(success("commands.switchy.import.success", literal(String.valueOf(importedPresets.keySet().stream().filter(Predicate.not(presets.getPresetNames()::contains)).toList().size())), literal(String.valueOf(importedPresets.keySet().stream().filter(presets.getPresetNames()::contains).toList().size()))));
		return SwitchyFeedbackStatus.SUCCESS;
	}
}

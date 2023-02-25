package folk.sisby.switchy.api.module;

import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;

import static folk.sisby.switchy.util.Feedback.translatable;


/**
 * @param isDefault whether the module should be enabled for new players
 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}
 * @param applyDependencies which other modules need to be applied to the player before this one
 * @param uniqueIds a collection of unique IDs that cannot collide with other added modules
 * @param disableConfirmation a text warning to show when the user attempts to disable the module
 * @see SwitchyModuleRegistry
 * Static settings for a module, set during registration.
 */
public record SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) {
	private static final String KEY_DEFAULT_DISABLE = "commands.switchy.module.disable.warn";

	/**
	 * @param isDefault whether the module should be enabled for new players
	 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable)  {
		this(isDefault, editable, Set.of(), Set.of(), translatable(KEY_DEFAULT_DISABLE));
	}

	/**
	 * @param isDefault whether the module should be enabled for new players
	 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}
	 * @param applyDependencies which other modules need to be applied to the player before this one
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies) {
		this(isDefault, editable, applyDependencies, Set.of(), translatable(KEY_DEFAULT_DISABLE));
	}

	/**
	 * @param isDefault whether the module should be enabled for new players
	 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}
	 * @param applyDependencies which other modules need to be applied to the player before this one
	 * @param uniqueIds a collection of unique IDs that cannot collide with other added modules
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds)  {
		this(isDefault, editable, applyDependencies, uniqueIds, translatable(KEY_DEFAULT_DISABLE));
	}

	/**
	 * @param isDefault whether the module should be enabled for new players
	 * @param editable permissions for cold-editing the module, see {@link SwitchyModuleEditable}
	 * @param applyDependencies which other modules need to be applied to the player before this one
	 * @param disableConfirmation a text warning to show when the user attempts to disable the module
	 */
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, MutableText disableConfirmation) {
		this(isDefault, editable, applyDependencies, Set.of(), disableConfirmation);
	}
}

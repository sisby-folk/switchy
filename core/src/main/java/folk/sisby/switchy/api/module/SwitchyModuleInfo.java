package folk.sisby.switchy.api.module;

import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;

import static folk.sisby.switchy.util.Feedback.translatable;

public record SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds, MutableText disableConfirmation) {
	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable)  {
		this(isDefault, editable, Set.of(), Set.of(), translatable("commands.switchy.module.disable.warn"));
	}

	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies) {
		this(isDefault, editable, applyDependencies, Set.of(), translatable("commands.switchy.module.disable.warn"));
	}

	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, Collection<Identifier> uniqueIds)  {
		this(isDefault, editable, applyDependencies, uniqueIds, translatable("commands.switchy.module.disable.warn"));
	}

	public SwitchyModuleInfo(boolean isDefault, SwitchyModuleEditable editable, Collection<Identifier> applyDependencies, MutableText disableConfirmation) {
		this(isDefault, editable, applyDependencies, Set.of(), disableConfirmation);
	}
}

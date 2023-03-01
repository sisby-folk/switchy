package folk.sisby.switchy.client.screen;

import folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;


public class PresetManagementScreen extends BaseUIModelScreen<FlowLayout> {
	private final SwitchyDisplayPresets displayPresets;

	public PresetManagementScreen(SwitchyDisplayPresets displayPresets) {
		super(FlowLayout.class, DataSource.asset(new Identifier("switchy", "preset_management_model")));
		this.displayPresets = displayPresets;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		rootComponent.gap(2);
		VerticalFlowLayout presetsFlow = rootComponent.childById(VerticalFlowLayout.class, "presetsFlow");
		presetsFlow.gap(2);
		refreshPresetFlow(presetsFlow);
		VerticalFlowLayout disabledModulesFlow = rootComponent.childById(VerticalFlowLayout.class, "disabledFlow");
		VerticalFlowLayout enabledModulesFlow = rootComponent.childById(VerticalFlowLayout.class, "enabledFlow");
		refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
		ScrollContainer<VerticalFlowLayout> presetsTab = rootComponent.childById(ScrollContainer.class, "presets-tab");
		VerticalFlowLayout modulesTab = rootComponent.childById(VerticalFlowLayout.class, "modules-tab");
		ScrollContainer<VerticalFlowLayout> dataTab = rootComponent.childById(ScrollContainer.class, "data-tab");
		VerticalFlowLayout panel = rootComponent.childById(VerticalFlowLayout.class, "panel");
		ButtonComponent presetsTabButton = rootComponent.childById(ButtonComponent.class, "presetsTabButton");
		ButtonComponent modulesTabButton = rootComponent.childById(ButtonComponent.class, "modulesTabButton");
		ButtonComponent dataTabButton = rootComponent.childById(ButtonComponent.class, "dataTabButton");


		// Add preset button
		rootComponent.childById(ButtonComponent.class, "newPreset").onPress(buttonComponent -> {
			presetsFlow.child(getRenameLayout(presetsFlow, null));
		});
		// Tab Buttons
		presetsTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(presetsTab);
		});
		modulesTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(modulesTab);
		});
		dataTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(dataTab);
		});
		panel.removeChild(presetsTab);
		panel.removeChild(dataTab);
	}

	private void refreshPresetFlow(VerticalFlowLayout presetsFlow) {
		presetsFlow.clearChildren();
		displayPresets.getPresets().forEach((name, preset) -> {
			HorizontalFlowLayout presetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent presetName = Components.label(Text.literal(name));
			presetName.horizontalSizing(Sizing.fill(54));
			ButtonComponent renameButton = Components.button(Text.literal("Rename"), b -> {
				presetFlow.clearChildren();
				presetFlow.child(getRenameLayout(presetsFlow, name));
			});
			renameButton.horizontalSizing(Sizing.fill(22));
			ButtonComponent deleteButton = Components.button(Text.literal("Delete"), b -> {
				displayPresets.deletePreset(name);
				refreshPresetFlow(presetsFlow);
			});
			deleteButton.horizontalSizing(Sizing.fill(22));
			presetFlow.child(presetName);
			presetFlow.child(renameButton);
			presetFlow.child(deleteButton);
			presetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
			presetFlow.gap(2);
			presetsFlow.child(presetFlow);
		});
	}

	private void refreshModulesFlow(VerticalFlowLayout disabledModulesFlow, VerticalFlowLayout enabledModulesFlow)
	{
		disabledModulesFlow.clearChildren();
		enabledModulesFlow.clearChildren();
		// Disabled Modules
		displayPresets.getDisabledModules().forEach(module -> {
			HorizontalFlowLayout moduleFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent moduleName = Components.label(Text.literal(module.toString()));
			moduleName.horizontalSizing(Sizing.fill(68));
			ButtonComponent enableButton = Components.button(Text.literal("Enable"), b -> {
				displayPresets.enableModule(module);
				refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
			});
			enableButton.horizontalSizing(Sizing.fill(28));
			moduleFlow.child(moduleName);
			moduleFlow.child(enableButton);
			moduleFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
			moduleFlow.gap(2);
			disabledModulesFlow.child(moduleFlow);
		});
		// Enabled Modules
		displayPresets.getEnabledModules().forEach(module -> {
			HorizontalFlowLayout moduleFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent moduleName = Components.label(Text.literal(module.toString()));
			moduleName.horizontalSizing(Sizing.fill(68));
			ButtonComponent disableButton = Components.button(Text.literal("Disable"), b -> {
				displayPresets.disableModule(module);
				refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
			});
			disableButton.horizontalSizing(Sizing.fill(28));
			moduleFlow.child(moduleName);
			moduleFlow.child(disableButton);
			moduleFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
			moduleFlow.gap(2);
			enabledModulesFlow.child(moduleFlow);
		});
	}

	private HorizontalFlowLayout getRenameLayout(VerticalFlowLayout presetsFlow, @Nullable String presetName)
	{
		HorizontalFlowLayout renamePresetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		TextBoxComponent nameEntry = Components.textBox(Sizing.fill(54), (presetName != null) ? presetName : "Preset Name");
		renamePresetFlow.child(nameEntry);
		ButtonComponent confirmButton = Components.button(Text.literal("Confirm"), (presetName != null) ? b -> {
			if (!presetName.equals(nameEntry.getText())) displayPresets.renamePreset(presetName, nameEntry.getText());
			refreshPresetFlow(presetsFlow);
		}: b -> {
			displayPresets.newPreset(nameEntry.getText());
			refreshPresetFlow(presetsFlow);
		});
		confirmButton.horizontalSizing(Sizing.fill(22));
		ButtonComponent cancelButton = Components.button(Text.literal("Cancel"), b -> {
			refreshPresetFlow(presetsFlow);
		});
		cancelButton.horizontalSizing(Sizing.fill(22));
		renamePresetFlow.child(confirmButton);
		renamePresetFlow.child(cancelButton);
		renamePresetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		renamePresetFlow.gap(2);
		return renamePresetFlow;
	}
}

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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class PresetManagementScreen extends BaseUIModelScreen<FlowLayout> {
	private final SwitchyDisplayPresets displayPresets;
	private FlowLayout root;

	public PresetManagementScreen(SwitchyDisplayPresets displayPresets) {
		super(FlowLayout.class, DataSource.asset(new Identifier("switchy", "preset_management_model")));
		this.displayPresets = displayPresets;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.root = rootComponent;
		// Root
		root.gap(2);

		// Preset Tab
		ScrollContainer<VerticalFlowLayout> presetsTab = model.expandTemplate(ScrollContainer.class, "presets-tab", Map.of("id", "presetsTab"));
		VerticalFlowLayout presetsFlow = presetsTab.childById(VerticalFlowLayout.class, "presetsFlow");
		presetsFlow.gap(2);
		refreshPresetFlow(presetsFlow);
		presetsTab.childById(ButtonComponent.class, "newPreset").onPress(buttonComponent -> {
			presetsFlow.child(getRenameLayout(presetsFlow, null));
		});

		// Modules Tab
		VerticalFlowLayout modulesTab = model.expandTemplate(VerticalFlowLayout.class, "modules-tab", Map.of("id", "modulesTab"));
		VerticalFlowLayout disabledModulesFlow = modulesTab.childById(VerticalFlowLayout.class, "disabledFlow");
		VerticalFlowLayout enabledModulesFlow = modulesTab.childById(VerticalFlowLayout.class, "enabledFlow");
		refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);

		// Data Tab
		ScrollContainer<VerticalFlowLayout> dataTab = model.expandTemplate(ScrollContainer.class, "data-tab", Map.of("id", "dataTab"));


		// Header
		VerticalFlowLayout panel = root.childById(VerticalFlowLayout.class, "panel");
		ButtonComponent presetsTabButton = root.childById(ButtonComponent.class, "presetsTabButton");
		ButtonComponent modulesTabButton = root.childById(ButtonComponent.class, "modulesTabButton");
		ButtonComponent dataTabButton = root.childById(ButtonComponent.class, "dataTabButton");
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

		panel.child(presetsTab); // Default Tab
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
				openDialog(
						"OK",
						"Cancel",
						200,
						okButton -> {
							displayPresets.deletePreset(name);
							refreshPresetFlow(presetsFlow);
						},
						cancel -> {
						},
						List.of(Text.literal("Are you sure you want to delete: "), Text.of(preset.getName()))
				);
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

	private void refreshModulesFlow(VerticalFlowLayout disabledModulesFlow, VerticalFlowLayout enabledModulesFlow) {
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
				openDialog(
						"OK",
						"Cancel",
						200,
						okButton -> {
							displayPresets.disableModule(module);
							refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
						},
						cancel -> {
						},
						List.of(Text.literal("Are you sure you want to disable: "), Text.of(module.toString()))
				);

			});
			disableButton.horizontalSizing(Sizing.fill(28));
			moduleFlow.child(moduleName);
			moduleFlow.child(disableButton);
			moduleFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
			moduleFlow.gap(2);
			enabledModulesFlow.child(moduleFlow);
		});
	}

	private HorizontalFlowLayout getRenameLayout(VerticalFlowLayout presetsFlow, @Nullable String presetName) {
		HorizontalFlowLayout renamePresetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		TextBoxComponent nameEntry = Components.textBox(Sizing.fill(54), (presetName != null) ? presetName : "Preset Name");
		renamePresetFlow.child(nameEntry);
		ButtonComponent confirmButton = Components.button(Text.literal("Confirm"), (presetName != null) ? b -> {
			if (!presetName.equals(nameEntry.getText())) displayPresets.renamePreset(presetName, nameEntry.getText());
			refreshPresetFlow(presetsFlow);
		} : b -> {
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

	VerticalFlowLayout openDialog(String leftButtonText, String rightButtonText, int hSize, Consumer<ButtonComponent> leftButtonAction, Consumer<ButtonComponent> rightButtonAction, List<Text> messages) {
		VerticalFlowLayout dialog = model.expandTemplate(VerticalFlowLayout.class, "dialog-box", Map.of("leftText", leftButtonText, "rightText", rightButtonText, "hSize", String.valueOf(hSize)));
		VerticalFlowLayout messageFlow = dialog.childById(VerticalFlowLayout.class, "messageFlow");
		ButtonComponent leftButton = dialog.childById(ButtonComponent.class, "leftButton");
		ButtonComponent rightButton = dialog.childById(ButtonComponent.class, "rightButton");
		leftButton.onPress(leftB -> {
			leftButtonAction.accept(leftB);
			root.removeChild(dialog);
		});
		rightButton.onPress(rightB -> {
			rightButtonAction.accept(rightB);
			root.removeChild(dialog);
		});
		messages.forEach(m -> messageFlow.child(Components.label(m)));
		root.child(dialog);
		return dialog;
	}
}

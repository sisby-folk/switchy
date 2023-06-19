package folk.sisby.switchy.ui.screen;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.util.Feedback;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A screen for managing the server-side presets object from the client.
 * Intended as an alternative to core Switchy commands.
 *
 * @author Garden System
 * @since 2.0.0
 */
public class ManageScreen extends BaseUIModelScreen<FlowLayout> implements SwitchyScreen {

	private FlowLayout root;
	private ScrollContainer<?> presetsTab;
	private FlowLayout modulesTab;
	private FlowLayout dataTab;
	private FlowLayout loadingOverlay;
	private List<Identifier> includedModules = new ArrayList<>();
	private List<Identifier> availableModules = new ArrayList<>();
	private NbtCompound selectedFileNbt;
	private String focusedPresetName;
	private boolean isImporting = true;

	private SwitchyClientPresets presets;

	/**
	 * Constructs an instance of the screen.
	 */
	public ManageScreen() {
		super(FlowLayout.class, DataSource.asset(new Identifier("switchy", "preset_management_model")));
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	protected void build(FlowLayout rootComponent) {
		this.root = rootComponent;
		// Preset Tab
		presetsTab = model.expandTemplate(ScrollContainer.class, "presets-tab", Map.of("id", "presetsTab"));

		// Modules Tab
		modulesTab = model.expandTemplate(FlowLayout.class, "modules-tab", Map.of());

		// Data Tab
		dataTab = model.expandTemplate(FlowLayout.class, "data-tab", Map.of());
		ButtonComponent importToggle = dataTab.childById(ButtonComponent.class, "importToggleButton");
		ButtonComponent exportToggle = dataTab.childById(ButtonComponent.class, "exportToggleButton");
		FlowLayout actionFlow = dataTab.childById(FlowLayout.class, "actionButtons");
		ButtonComponent exportButton = dataTab.childById(ButtonComponent.class, "exportButton");
		ButtonComponent importButton = dataTab.childById(ButtonComponent.class, "importButton");
		importButton.onPress(b -> openDialog("Confirm", "Cancel", 200, confirmButton -> {
			lockScreen();
			SwitchyClientApi.importPresets(selectedFileNbt, availableModules, includedModules, SwitchyScreen::updatePresetScreens);
		}, cancelButton -> {
		}, List.of(Text.translatable("screen.switchy_ui.import.warn.info", Feedback.literal(String.valueOf(selectedFileNbt.getCompound(SwitchyPresetsData.KEY_PRESETS).getKeys().size())), Feedback.literal(String.valueOf(includedModules.size()))), Text.translatable("screen.switchy_ui.list.presets", Feedback.getHighlightedListText(selectedFileNbt.getCompound(SwitchyPresetsData.KEY_PRESETS).getKeys().stream().sorted().toList(), List.of(new Pair<>(presets.getPresetNames()::contains, Formatting.DARK_RED)))), Text.translatable("screen.switchy_ui.import.warn.collision"), Text.translatable("screen.switchy_ui.list.modules", Feedback.getIdListText(includedModules)))));
		exportButton.onPress(b -> openDialog("Confirm", "Cancel", 200, confirmButton -> {
			lockScreen();
			SwitchyClientApi.exportPresetsToFile(availableModules, null, (feedback, file) -> SwitchyScreen.updatePresetScreens(feedback, presets));
		}, cancelButton -> {
		}, List.of(Text.translatable("commands.switchy_client.export.confirm", Feedback.literal(String.valueOf(includedModules.size()))))));
		importToggle.onPress(b -> {
			isImporting = true;
			importToggle.active(false);
			exportToggle.active(true);
			actionFlow.removeChild((Component) exportButton);
			actionFlow.child((Component) importButton);
			updateDataMethod();
		});
		exportToggle.onPress(b -> {
			isImporting = false;
			exportToggle.active(false);
			importToggle.active(true);
			actionFlow.removeChild((Component) importButton);
			actionFlow.child((Component) exportButton);
			updateDataMethod();
		});
		importToggle.active(false);
		actionFlow.removeChild((Component) exportButton);


		FlowLayout sourceSelectorPlaceholder = dataTab.childById(FlowLayout.class, "sourceSelectorPlaceholder");
		sourceSelectorPlaceholder.child(getDropdownButton(getDropdown(sourceSelectorPlaceholder, List.of(Text.of("File")), text -> updateDataMethod()), Text.of("File")));

		// Header
		FlowLayout panel = root.childById(FlowLayout.class, "panel");
		ButtonComponent backButton = root.childById(ButtonComponent.class, "backButton");
		ButtonComponent presetsTabButton = root.childById(ButtonComponent.class, "presetsTabButton");
		ButtonComponent modulesTabButton = root.childById(ButtonComponent.class, "modulesTabButton");
		ButtonComponent dataTabButton = root.childById(ButtonComponent.class, "dataTabButton");
		backButton.onPress(buttonComponent -> {
			SwitchScreen switchScreen = new SwitchScreen();
			client.setScreen(switchScreen);
			switchScreen.updatePresets(presets);
		});
		presetsTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(presetsTab);
			presetsTabButton.active(false);
			modulesTabButton.active(true);
			dataTabButton.active(true);

		});
		modulesTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(modulesTab);
			presetsTabButton.active(true);
			modulesTabButton.active(false);
			dataTabButton.active(true);
		});
		dataTabButton.onPress(buttonComponent -> {
			panel.clearChildren();
			panel.child(dataTab);
			presetsTabButton.active(true);
			modulesTabButton.active(true);
			dataTabButton.active(false);
		});

		panel.child(presetsTab); // Default Tab
		presetsTabButton.active(false);
		lockScreen();
	}

	@Override
	public void updatePresets(SwitchyClientPresets clientPresets) {
		presets = clientPresets;
		refreshPresets();
	}


	// Generic Components
	private DropdownComponent getDropdownButton(DropdownComponent contextMenu, Text text) {
		DropdownComponent selectorButton = Components.dropdown(Sizing.content());
		selectorButton.text(text);
		selectorButton.margins(Insets.left(4));
		selectorButton.mouseDown().subscribe((x, y, b) -> {
			if (!contextMenu.hasParent()) {
				root.child(contextMenu.positioning(Positioning.absolute(selectorButton.x(), selectorButton.y() + selectorButton.height())));
			} else {
				root.removeChild(contextMenu);
			}
			return true;
		});
		return selectorButton;
	}

	DropdownComponent getDropdown(FlowLayout placeholder, List<Text> entries, Consumer<Text> onSelected) {
		DropdownComponent dropdown = Components.dropdown(Sizing.content());

		for (Text entry : entries) {
			dropdown.button(entry, dropdownComponent -> {
				root.removeChild(dropdown);
				placeholder.clearChildren();
				placeholder.child(getDropdownButton(dropdown, entry));
				onSelected.accept(entry);
			});
		}
		return dropdown;
	}

	private FlowLayout getRenameFlow(FlowLayout presetsFlow, @Nullable String presetName) {
		FlowLayout renamePresetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		TextBoxComponent nameEntry = Components.textBox(Sizing.fill(53), (presetName != null) ? presetName : "newPreset");
		nameEntry.setTextPredicate(s -> s.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString));
		this.focusOn(nameEntry);
		renamePresetFlow.child((Component) nameEntry);
		ButtonComponent confirmButton = Components.button(Text.literal("Confirm"), (presetName != null) ? b -> {
			if (!presetName.equals(nameEntry.getText())) {
				if (presets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
					presets.renamePreset(presetName, nameEntry.getText());
					focusedPresetName = null;
					refreshPresetFlow(presetsFlow);
					lockScreen();
					SwitchyClientApi.renamePreset(presetName, nameEntry.getText(), SwitchyScreen::updatePresetScreens);
				}
			} else {
				focusedPresetName = null;
				refreshPresetFlow(presetsFlow);
			}

		} : b -> {
			if (presets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
				presets.newPreset(nameEntry.getText());
				focusedPresetName = null;
				refreshPresetFlow(presetsFlow);
				lockScreen();
				SwitchyClientApi.newPreset(nameEntry.getText(), SwitchyScreen::updatePresetScreens);
			}

		});
		confirmButton.horizontalSizing(Sizing.fill(22));
		confirmButton.margins(Insets.vertical(1));
		ButtonComponent cancelButton = Components.button(Text.literal("Cancel"), b -> {
			focusedPresetName = null;
			refreshPresetFlow(presetsFlow);
		});
		cancelButton.horizontalSizing(Sizing.fill(22));
		renamePresetFlow.child((Component) confirmButton);
		renamePresetFlow.child((Component) cancelButton);
		renamePresetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		renamePresetFlow.gap(2);
		return renamePresetFlow;
	}

	private FlowLayout getPresetFlow(FlowLayout presetsFlow, @Nullable String name) {
		FlowLayout presetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		LabelComponent presetLabel = Components.label(Text.literal(name));
		presetLabel.horizontalSizing(Sizing.fill(54));
		ButtonComponent renameButton = Components.button(Text.literal("Rename"), b -> {
			focusedPresetName = name;
			refreshPresetFlow(presetsFlow);
		});
		renameButton.horizontalSizing(Sizing.fill(22));
		Consumer<ButtonComponent> deleteAction = b -> openDialog("OK", "Cancel", 200, okButton -> {
			presets.deletePreset(name);
			refreshPresetFlow(presetsFlow);
			lockScreen();
			SwitchyClientApi.deletePreset(name, SwitchyScreen::updatePresetScreens);
		}, cancel -> {
		}, List.of(Text.translatable("commands.switchy_client.delete.confirm", Feedback.literal(name)), Text.translatable("screen.switchy_ui.delete.warn"), Text.translatable("screen.switchy_ui.list.modules", presets.getEnabledModuleText())));
		ButtonComponent deleteButton = Components.button(Text.literal("Delete"), deleteAction);
		deleteButton.margins(Insets.vertical(1));
		deleteButton.horizontalSizing(Sizing.fill(22));
		deleteButton.active(!presets.getCurrentPresetName().equals(name));
		presetFlow.child(presetLabel);
		presetFlow.child((Component) renameButton);
		presetFlow.child((Component) deleteButton);
		presetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		presetFlow.gap(2);
		presetFlow.padding(Insets.of(1));
		presetFlow.surface(Surface.flat(0xFF1E1E1E));
		return presetFlow;
	}

	FlowLayout getModuleFlow(Identifier id, @Nullable Text labelTooltip, BiConsumer<ButtonComponent, Identifier> buttonAction, boolean enabled, Text buttonText, @Nullable Text buttonTooltip, int labelSize) {
		FlowLayout moduleFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		LabelComponent moduleName = Components.label(Text.literal(id.getPath()));
		Text namespaceText = Text.literal(id.getNamespace()).setStyle(Feedback.FORMAT_INFO.getLeft());
		moduleName.tooltip(labelTooltip != null ? List.of(namespaceText, labelTooltip) : List.of(namespaceText));
		moduleName.horizontalSizing(Sizing.fixed(labelSize));
		ButtonComponent enableButton = Components.button(buttonText, b -> buttonAction.accept(b, id));
		if (buttonTooltip != null) enableButton.tooltip(buttonTooltip);
		enableButton.active(enabled);
		enableButton.horizontalSizing(Sizing.content());
		moduleFlow.child(moduleName);
		moduleFlow.child((Component) enableButton);
		moduleFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		moduleFlow.gap(2);
		moduleFlow.padding(Insets.of(1));
		moduleFlow.surface(Surface.flat(0xFF1E1E1E));
		return moduleFlow;
	}

	// Special Components
	@SuppressWarnings("ConstantConditions")
	void openDialog(String leftButtonText, String rightButtonText, int hSize, Consumer<ButtonComponent> leftButtonAction, Consumer<ButtonComponent> rightButtonAction, List<Text> messages) {
		FlowLayout dialog = model.expandTemplate(FlowLayout.class, "dialog-box", Map.of("leftText", leftButtonText, "rightText", rightButtonText, "hSize", String.valueOf(hSize)));
		OverlayContainer<FlowLayout> overlay = Containers.overlay(dialog).closeOnClick(false);
		overlay.mouseDown().subscribe((x, y, b) -> true); // eat all input
		FlowLayout messageFlow = dialog.childById(FlowLayout.class, "messageFlow");
		ButtonComponent leftButton = dialog.childById(ButtonComponent.class, "leftButton");
		ButtonComponent rightButton = dialog.childById(ButtonComponent.class, "rightButton");
		leftButton.onPress(leftB -> {
			leftButtonAction.accept(leftB);
			root.removeChild(overlay);
		});
		rightButton.onPress(rightB -> {
			rightButtonAction.accept(rightB);
			root.removeChild(overlay);
		});
		messages.forEach(m -> {
			LabelComponent message = Components.label(m);
			message.horizontalSizing(Sizing.fill(90));
			messageFlow.child(message);
		});
		root.child(overlay);
	}

	void lockScreen() {
		if (loadingOverlay == null) {
			FlowLayout overlay = model.expandTemplate(FlowLayout.class, "loading-overlay", Map.of());
			overlay.mouseDown().subscribe((x, y, b) -> true); // eat all input
			loadingOverlay = overlay;
			root.child(loadingOverlay);
		}
	}

	// Refreshers
	@SuppressWarnings({"ConstantConditions"})
	private void refreshPresets() {
		// Presets Tab
		FlowLayout presetsFlow = presetsTab.childById(FlowLayout.class, "presetsFlow");
		refreshPresetFlow(presetsFlow);
		presetsTab.childById(ButtonComponent.class, "newPreset").onPress(buttonComponent -> {
			focusedPresetName = "";
			refreshPresetFlow(presetsFlow);
		});

		//Modules Tab
		FlowLayout disabledModulesFlow = modulesTab.childById(FlowLayout.class, "leftModulesFlow");
		FlowLayout enabledModulesFlow = modulesTab.childById(FlowLayout.class, "rightModulesFlow");
		refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);

		//Data Tab
		updateDataMethod();

		// Clear locked state
		if (loadingOverlay != null) {
			root.removeChild(loadingOverlay);
			loadingOverlay = null;
		}
	}

	private void refreshPresetFlow(FlowLayout presetsFlow) {
		presetsFlow.clearChildren();
		FlowLayout focusedFlow = null;
		for (String name : presets.getPresets().keySet()) {
			FlowLayout presetFlow = name.equals(focusedPresetName) ? getRenameFlow(presetsFlow, name) : getPresetFlow(presetsFlow, name);
			if (name.equals(focusedPresetName)) focusedFlow = presetFlow;
			presetsFlow.child(presetFlow);
		}
		if ("".equals(focusedPresetName)) {
			focusedFlow = getRenameFlow(presetsFlow, null);
			presetsFlow.child(focusedFlow);
		}
		if (focusedFlow != null) presetsTab.scrollTo(focusedFlow);
	}

	private void refreshModulesFlow(FlowLayout disabledModulesFlow, FlowLayout enabledModulesFlow) {
		disabledModulesFlow.clearChildren();
		enabledModulesFlow.clearChildren();
		int labelSize = 100;
		disabledModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
		}, true, Text.literal("Enable"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));
		enabledModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
		}, true, Text.literal("Disable"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));

		// Disabled Modules
		presets.getDisabledModules().forEach(module -> disabledModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
			presets.enableModule(id);
			refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
			lockScreen();
			SwitchyClientApi.enableModule(id, SwitchyScreen::updatePresetScreens);
		}, true, Text.literal("Enable"), presets.getModuleInfo().get(module).descriptionWhenEnabled(), labelSize)));
		// Enabled Modules
		presets.getEnabledModules().forEach(module -> enabledModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> openDialog("OK", "Cancel", 200, okButton -> {
			presets.disableModule(id);
			refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
			lockScreen();
			SwitchyClientApi.disableModule(id, SwitchyScreen::updatePresetScreens);
		}, cancel -> {
		}, List.of(Text.translatable("commands.switchy_client.disable.confirm", Feedback.literal(id.getPath())), Text.translatable("screen.switchy_ui.disable.warn", presets.getModuleInfo().get(id).deletionWarning()))), true, Text.literal("Disable"), presets.getModuleInfo().get(module).descriptionWhenDisabled(), labelSize)));

	}

	@SuppressWarnings("ConstantConditions")
	void updateDataMethod() {
		FlowLayout availableModulesFlow = dataTab.childById(FlowLayout.class, "leftModulesFlow");
		FlowLayout includedModulesFlow = dataTab.childById(FlowLayout.class, "rightModulesFlow");
		List<Text> fileNames = new ArrayList<>();
		Map<String, NbtCompound> importFiles = new HashMap<>();
		FlowLayout fileSelectorPlaceholder = dataTab.childById(FlowLayout.class, "fileSelectorPlaceholder");
		fileSelectorPlaceholder.clearChildren();
		if (isImporting) {
			SwitchyClientApi.getImportableFiles().forEach(file -> {
				try {
					NbtCompound nbt = NbtIo.readCompressed(file);
					nbt.putString("filename", FilenameUtils.getBaseName(file.getName()));

					String name = file.getName();
					String baseName = FileNameUtils.getBaseName(name);
					importFiles.put(baseName, nbt);
				} catch (IOException ignored) {
				}
			});
			importFiles.keySet().stream().map(Text::of).forEach(fileNames::add);
		} else {
			fileNames.add(Text.of("New file..."));
		}

		includedModules = isImporting ? new ArrayList<>() : new ArrayList<>(presets.getEnabledModules());
		availableModules = new ArrayList<>();

		DropdownComponent dropdown = getDropdown(fileSelectorPlaceholder, fileNames, isImporting ? text -> {
			selectedFileNbt = importFiles.get(text.getString());
			includedModules = selectedFileNbt.getList(SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).filter(id -> {
				SwitchyModuleInfo moduleInfo = presets.getModuleInfo().get(id);
				if (moduleInfo == null) return false;
				return moduleInfo.editable() == SwitchyModuleEditable.ALLOWED || moduleInfo.editable() == SwitchyModuleEditable.ALWAYS_ALLOWED;
			}).collect(Collectors.toList());
			availableModules = selectedFileNbt.getList(SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).filter(id -> {
				SwitchyModuleInfo moduleInfo = presets.getModuleInfo().get(id);
				if (moduleInfo == null) return true;
				return moduleInfo.editable() == SwitchyModuleEditable.OPERATOR || moduleInfo.editable() == SwitchyModuleEditable.NEVER;
			}).collect(Collectors.toList());
			refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
		} : text -> {
			includedModules = new ArrayList<>(presets.getEnabledModules());
			availableModules = new ArrayList<>();
			refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
		});
		fileSelectorPlaceholder.child(getDropdownButton(dropdown, isImporting ? Text.of("Select a file...") : Text.of("New file...")));

		refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
	}

	void refreshDataModulesFlow(FlowLayout availableModulesFlow, FlowLayout includedModulesFlow, List<Identifier> availableModules, List<Identifier> includedModules) {
		availableModulesFlow.clearChildren();
		includedModulesFlow.clearChildren();
		int labelSize = 100;
		availableModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
		}, false, Text.literal("Add"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));
		includedModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
		}, false, Text.literal("Remove"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));

		if (isImporting) {
			// Available Modules
			List<Identifier> noPermissionModules = availableModules.stream().filter(m -> presets.getPermissionLevel() < 2 && presets.getModuleInfo().containsKey(m) && presets.getModuleInfo().get(m).editable() == SwitchyModuleEditable.OPERATOR).toList();
			List<Identifier> neverModules = availableModules.stream().filter(m -> presets.getModuleInfo().containsKey(m) && presets.getModuleInfo().get(m).editable() == SwitchyModuleEditable.NEVER).toList();
			List<Identifier> notInstalledModules = availableModules.stream().filter(m -> !presets.getModuleInfo().containsKey(m)).toList();
			List<Identifier> includableModules = availableModules.stream().filter(m -> !noPermissionModules.contains(m) && !neverModules.contains(m) && !notInstalledModules.contains(m)).toList();

			includableModules.forEach(module -> availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				includedModules.add(module);
				availableModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, true, Text.literal("Add"), Text.literal("Include in import"), labelSize)));
			noPermissionModules.forEach(module -> availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				includedModules.add(module);
				availableModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, false, Text.literal("Add"), Text.literal("Requires Operator permissions"), labelSize)));
			neverModules.forEach(module -> availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				includedModules.add(module);
				availableModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, false, Text.literal("Add"), Text.literal("This type of module cannot be imported"), labelSize)));
			notInstalledModules.forEach(module -> availableModulesFlow.child(getModuleFlow(module, null, (b, id) -> {
				includedModules.add(module);
				availableModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, false, Text.literal("Add"), Text.literal("The server does not have this module installed"), labelSize)));
			// Included Modules
			includedModules.forEach(module -> includedModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				availableModules.add(module);
				includedModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, true, Text.literal("Remove"), Text.literal("Remove from import"), labelSize)));
		} else {
			availableModules.forEach(module -> availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				includedModules.add(module);
				availableModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, true, Text.literal("Add"), Text.literal("Include in export"), labelSize)));
			includedModules.forEach(module -> includedModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				availableModules.add(module);
				includedModules.remove(module);
				refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
			}, true, Text.literal("Remove"), Text.literal("Remove from export"), labelSize)));
		}

	}
}

package folk.sisby.switchy.ui.screen;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.SwitchyClientServerNetworking;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.client.util.SwitchyFiles;
import folk.sisby.switchy.util.Feedback;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
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
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class PresetManagementScreen extends BaseUIModelScreen<FlowLayout> implements SwitchyDisplayScreen {

	private FlowLayout root;
	private ScrollContainer<VerticalFlowLayout> presetsTab;
	private HorizontalFlowLayout modulesTab;
	private VerticalFlowLayout dataTab;
	private VerticalFlowLayout loadingOverlay;
	private List<Identifier> includedModules = new ArrayList<>();
	private List<Identifier> availableModules = new ArrayList<>();
	private NbtCompound selectedFileNbt;
	private boolean isImporting = true;

	private SwitchyClientPresets presets;

	public PresetManagementScreen() {
		super(FlowLayout.class, DataSource.asset(new Identifier("switchy", "preset_management_model")));
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.root = rootComponent;
		// Preset Tab
		presetsTab = model.expandTemplate(ScrollContainer.class, "presets-tab", Map.of("id", "presetsTab"));
		VerticalFlowLayout presetsFlow = presetsTab.childById(VerticalFlowLayout.class, "presetsFlow");
		presetsFlow.gap(2);


		// Modules Tab
		modulesTab = model.expandTemplate(HorizontalFlowLayout.class, "modules-tab", Map.of());

		// Data Tab
		dataTab = model.expandTemplate(VerticalFlowLayout.class, "data-tab", Map.of());
		ButtonComponent importToggle = dataTab.childById(ButtonComponent.class, "importToggleButton");
		ButtonComponent exportToggle = dataTab.childById(ButtonComponent.class, "exportToggleButton");
		HorizontalFlowLayout actionFlow = dataTab.childById(HorizontalFlowLayout.class, "actionButtons");
		ButtonComponent exportButton = dataTab.childById(ButtonComponent.class, "exportButton");
		ButtonComponent importButton = dataTab.childById(ButtonComponent.class, "importButton");
		importButton.onPress(b -> {
			openDialog("Confirm", "Cancel", 200, confirmButton -> {
				lockScreen();
				SwitchyClientApi.importPresets(selectedFileNbt, availableModules, includedModules, SwitchyDisplayScreen::updatePresetScreens);
			}, cancelButton -> {
			}, List.of(Text.translatable("commands.switchy.import.warn.info", Feedback.literal(String.valueOf(selectedFileNbt.getCompound(SwitchyPresetsData.KEY_PRESETS).getKeys().size())), Feedback.literal(String.valueOf(includedModules.size()))), Text.translatable("commands.switchy.list.presets", Feedback.getHighlightedListText(selectedFileNbt.getCompound(SwitchyPresetsData.KEY_PRESETS).getKeys().stream().sorted().toList(), List.of(new Pair<>(presets.getPresetNames()::contains, Formatting.DARK_RED)))), Text.translatable("commands.switchy.import.warn.collision"), Text.translatable("commands.switchy.list.modules", Feedback.getIdListText(includedModules))));
		});
		exportButton.onPress(b -> {
			openDialog("Confirm", "Cancel", 200, confirmButton -> {
				lockScreen();
				SwitchyClientApi.exportPresets(availableModules, (feedback, nbt) -> {
					SwitchyFiles.exportPresetsToFile(MinecraftClient.getInstance(), nbt);
					SwitchyDisplayScreen.updatePresetScreens(feedback, presets);
				});
			}, cancelButton -> {
			}, List.of(Text.translatable("commands.switchy_client.export.confirm", String.valueOf(includedModules.size()))));
		});
		importToggle.onPress(b -> {
			isImporting = true;
			importToggle.active(false);
			exportToggle.active(true);
			actionFlow.removeChild(exportButton);
			actionFlow.child(importButton);
			updateDataMethod();
		});
		exportToggle.onPress(b -> {
			isImporting = false;
			exportToggle.active(false);
			importToggle.active(true);
			actionFlow.removeChild(importButton);
			actionFlow.child(exportButton);
			updateDataMethod();
		});
		importToggle.active(false);
		actionFlow.removeChild(exportButton);


		VerticalFlowLayout sourceSelectorPlaceholder = dataTab.childById(VerticalFlowLayout.class, "sourceSelectorPlaceholder");
		sourceSelectorPlaceholder.child(getDropdownButton(getDropdown(sourceSelectorPlaceholder, List.of(Text.of("File")), text -> updateDataMethod()), Text.of("File")));

		// Header
		VerticalFlowLayout panel = root.childById(VerticalFlowLayout.class, "panel");
		ButtonComponent backButton = root.childById(ButtonComponent.class, "backButton");
		ButtonComponent presetsTabButton = root.childById(ButtonComponent.class, "presetsTabButton");
		ButtonComponent modulesTabButton = root.childById(ButtonComponent.class, "modulesTabButton");
		ButtonComponent dataTabButton = root.childById(ButtonComponent.class, "dataTabButton");
		backButton.onPress(buttonComponent -> {
			client.setScreen(new SwitchScreen());
			ClientPlayNetworking.send(SwitchyClientServerNetworking.C2S_REQUEST_CLIENT_PRESETS, PacketByteBufs.empty());
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
	public void updatePresets(SwitchyClientPresets displayPresets) {
		presets = displayPresets;
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

	private HorizontalFlowLayout getRenameLayout(VerticalFlowLayout presetsFlow, @Nullable String presetName) {
		HorizontalFlowLayout renamePresetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		TextBoxComponent nameEntry = Components.textBox(Sizing.fill(54), (presetName != null) ? presetName : "newPreset");
		nameEntry.setTextPredicate(s -> s.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString));
		this.setInitialFocus(nameEntry);
		renamePresetFlow.child(nameEntry);
		ButtonComponent confirmButton = Components.button(Text.literal("Confirm"), (presetName != null) ? b -> {
			if (!presetName.equals(nameEntry.getText())) {
				if (presets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
					presets.renamePreset(presetName, nameEntry.getText());
					refreshPresetFlow(presetsFlow);
					lockScreen();
					SwitchyClientApi.renamePreset(presetName, nameEntry.getText(), SwitchyDisplayScreen::updatePresetScreens);
				}
			} else {
				refreshPresetFlow(presetsFlow);
			}

		} : b -> {
			if (presets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
				presets.newPreset(nameEntry.getText());
				refreshPresetFlow(presetsFlow);
				lockScreen();
				SwitchyClientApi.newPreset(nameEntry.getText(), SwitchyDisplayScreen::updatePresetScreens);
			}

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

	HorizontalFlowLayout getModuleFlow(Identifier id, @Nullable Text labelTooltip, BiConsumer<ButtonComponent, Identifier> buttonAction, boolean enabled, Text buttonText, @Nullable Text buttonTooltip, int labelSize) {
		HorizontalFlowLayout moduleFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		LabelComponent moduleName = Components.label(Text.literal(id.toString()));
		if (labelTooltip != null) moduleName.tooltip(labelTooltip);
		moduleName.horizontalSizing(Sizing.fixed(labelSize));
		ButtonComponent enableButton = Components.button(buttonText, b -> {
			buttonAction.accept(b, id);
		});
		if (buttonTooltip != null) enableButton.tooltip(buttonTooltip);
		enableButton.active(enabled);
		enableButton.horizontalSizing(Sizing.content());
		moduleFlow.child(moduleName);
		moduleFlow.child(enableButton);
		moduleFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
		moduleFlow.gap(2);
		return moduleFlow;
	}

	// Special Components

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
		messageFlow.gap(2);
		messages.forEach(m -> {
			LabelComponent message = Components.label(m);
			message.horizontalSizing(Sizing.fill(90));
			messageFlow.child(message);
		});

		root.child(dialog);
		return dialog;
	}

	void lockScreen() {
		if (loadingOverlay == null) {
			loadingOverlay = model.expandTemplate(VerticalFlowLayout.class, "loading-overlay", Map.of());
			root.child(loadingOverlay);
		}
	}

	// Refreshers

	private void refreshPresets() {
		// Presets Tab
		VerticalFlowLayout presetsFlow = presetsTab.childById(VerticalFlowLayout.class, "presetsFlow");
		refreshPresetFlow(presetsFlow);
		presetsTab.childById(ButtonComponent.class, "newPreset").onPress(buttonComponent -> {
			presetsFlow.child(getRenameLayout(presetsFlow, null));
		});

		//Modules Tab
		VerticalFlowLayout disabledModulesFlow = modulesTab.childById(VerticalFlowLayout.class, "leftModulesFlow");
		VerticalFlowLayout enabledModulesFlow = modulesTab.childById(VerticalFlowLayout.class, "rightModulesFlow");
		refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);

		//Data Tab
		updateDataMethod();

		// Clear locked state
		if (loadingOverlay != null) {
			root.removeChild(loadingOverlay);
			loadingOverlay = null;
		}
	}

	private void refreshPresetFlow(VerticalFlowLayout presetsFlow) {
		presetsFlow.clearChildren();
		presets.getPresets().forEach((name, preset) -> {
			HorizontalFlowLayout presetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent presetName = Components.label(Text.literal(name));
			presetName.horizontalSizing(Sizing.fill(54));
			ButtonComponent renameButton = Components.button(Text.literal("Rename"), b -> {
				presetFlow.clearChildren();
				presetFlow.child(getRenameLayout(presetsFlow, name));
			});
			renameButton.horizontalSizing(Sizing.fill(22));
			Consumer<ButtonComponent> deleteAction = b -> {
				openDialog("OK", "Cancel", 200, okButton -> {
					presets.deletePreset(name);
					refreshPresetFlow(presetsFlow);
					lockScreen();
					SwitchyClientApi.deletePreset(name, SwitchyDisplayScreen::updatePresetScreens);
				}, cancel -> {
				}, List.of(Text.translatable("commands.switchy_client.delete.confirm", name), Text.translatable("commands.switchy.delete.warn"), Text.translatable("commands.switchy.list.modules", presets.getEnabledModuleText())));
			};
			ButtonComponent deleteButton = Components.button(Text.literal("Delete"), deleteAction);
			deleteButton.horizontalSizing(Sizing.fill(22));
			deleteButton.active(!presets.getCurrentPresetName().equals(name));
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
		int labelSize = 100;
		disabledModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
		}, true, Text.literal("Enable"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));
		enabledModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
		}, true, Text.literal("Disable"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));

		// Disabled Modules
		presets.getDisabledModules().forEach(module -> {
			disabledModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				presets.enableModule(id);
				refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
				lockScreen();
				SwitchyClientApi.enableModule(id, SwitchyDisplayScreen::updatePresetScreens);
			}, true, Text.literal("Enable"), presets.getModuleInfo().get(module).descriptionWhenEnabled(), labelSize));
		});
		// Enabled Modules
		presets.getEnabledModules().forEach(module -> {
			enabledModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				openDialog("OK", "Cancel", 200, okButton -> {
					presets.disableModule(id);
					refreshModulesFlow(disabledModulesFlow, enabledModulesFlow);
					lockScreen();
					SwitchyClientApi.disableModule(id, SwitchyDisplayScreen::updatePresetScreens);
				}, cancel -> {
				}, List.of(Text.translatable("commands.switchy_client.disable.confirm", id.toString()), Text.translatable("commands.switchy.module.disable.warn", presets.getModuleInfo().get(id).deletionWarning())));
			}, true, Text.literal("Disable"), presets.getModuleInfo().get(module).descriptionWhenDisabled(), labelSize));
		});
	}

	void updateDataMethod() {
		VerticalFlowLayout availableModulesFlow = dataTab.childById(VerticalFlowLayout.class, "leftModulesFlow");
		VerticalFlowLayout includedModulesFlow = dataTab.childById(VerticalFlowLayout.class, "rightModulesFlow");
		List<Text> fileNames = new ArrayList<>();
		Map<String, NbtCompound> importFiles = new HashMap<>();
		VerticalFlowLayout fileSelectorPlaceholder = dataTab.childById(VerticalFlowLayout.class, "fileSelectorPlaceholder");
		fileSelectorPlaceholder.clearChildren();
		if (isImporting) {
			File[] fileArray = new File(SwitchyClient.EXPORT_PATH).listFiles((dir, name) -> FileNameUtils.getExtension(name).equalsIgnoreCase("dat"));
			if (fileArray != null) {
				for (File file : fileArray) {
					try {
						NbtCompound nbt = NbtIo.readCompressed(file);
						nbt.putString("filename", FilenameUtils.getBaseName(file.getName()));

						String name = file.getName();
						String baseName = FileNameUtils.getBaseName(name);
						importFiles.put(baseName, nbt);
					} catch (IOException ignored) {
					}
				}
				importFiles.keySet().stream().map(Text::of).forEach(fileNames::add);
			}
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

	void refreshDataModulesFlow(VerticalFlowLayout availableModulesFlow, VerticalFlowLayout includedModulesFlow, List<Identifier> availableModules, List<Identifier> includedModules) {
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

			includableModules.forEach(module -> {
				availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, true, Text.literal("Add"), Text.literal("Include in import"), labelSize));
			});
			noPermissionModules.forEach(module -> {
				availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, false, Text.literal("Add"), Text.literal("Requires Operator permissions"), labelSize));
			});
			neverModules.forEach(module -> {
				availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, false, Text.literal("Add"), Text.literal("This type of module cannot be imported"), labelSize));
			});
			notInstalledModules.forEach(module -> {
				availableModulesFlow.child(getModuleFlow(module, null, (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, false, Text.literal("Add"), Text.literal("The server does not have this module installed"), labelSize));
			});
			// Included Modules
			includedModules.forEach(module -> {
				includedModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					availableModules.add(module);
					includedModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, true, Text.literal("Remove"), Text.literal("Remove from import"), labelSize));
			});
		} else {
			availableModules.forEach(module -> {
				availableModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, true, Text.literal("Add"), Text.literal("Include in export"), labelSize));
			});
			includedModules.forEach(module -> {
				includedModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					availableModules.add(module);
					includedModules.remove(module);
					refreshDataModulesFlow(availableModulesFlow, includedModulesFlow, availableModules, includedModules);
				}, true, Text.literal("Remove"), Text.literal("Remove from export"), labelSize));
			});
		}

	}
}

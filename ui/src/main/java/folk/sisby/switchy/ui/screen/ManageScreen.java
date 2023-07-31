package folk.sisby.switchy.ui.screen;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.presets.SwitchyPresetsData;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.ui.component.DialogOverlayComponent;
import folk.sisby.switchy.ui.component.LockableFlowLayout;
import folk.sisby.switchy.ui.component.TabLayout;
import folk.sisby.switchy.util.Feedback;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
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
public class ManageScreen extends BaseOwoScreen<LockableFlowLayout> implements SwitchyScreen {

	private PresetsTabScroll presetsTab;
	private ModulesTab modulesTab;
	private DataTabTabLayout dataTab;

	private SwitchyClientPresets presets;

	/**
	 * Constructs an instance of the screen.
	 */
	public ManageScreen() {
		super();
	}

	@Override
	protected @NotNull OwoUIAdapter<LockableFlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, LockableFlowLayout::new);
	}

	public class PresetsTabScroll extends ScrollContainer<VerticalFlowLayout> {
		public PresetsTabScroll() {
			super(ScrollDirection.VERTICAL, Sizing.content(), Sizing.fixed(180), new PresetsTabFlow());
			this.surface(Surface.flat(0xFF141414).and(Surface.outline(0xFF202020)));
			this.margins(Insets.of(10));
		}

		@Override
		public PresetsTabFlow child() {
			return (PresetsTabFlow) child;
		}
	}

	@Override
	protected void build(LockableFlowLayout root) {
		// Preset Tab
		presetsTab = new PresetsTabScroll();

		// Modules Tab
		modulesTab = new ModulesTab();

		// Data Tab
		dataTab = new DataTabTabLayout();

		// Tab Setup & Back Button
		ButtonComponent backButton = Components.button(Text.translatable("screen.switchy.manage.back"), buttonComponent -> {
			SwitchScreen switchScreen = new SwitchScreen();
			if (client != null) client.setScreen(switchScreen);
			switchScreen.updatePresets(presets);
		});
		backButton.margins(Insets.right(10));

		root.child(new ManageTabLayout(List.of((Component) backButton),
			new TabLayout.Tab(Text.translatable("screen.switchy.manage.presets.button"), presetsTab),
			new TabLayout.Tab(Text.translatable("screen.switchy.manage.modules.button"), modulesTab),
			new TabLayout.Tab(Text.translatable("screen.switchy.manage.data.button"), dataTab)
		));
		root.lock();
	}

	@Override
	public void updatePresets(SwitchyClientPresets clientPresets) {
		presets = clientPresets;
		refreshPresets();
	}

	void openDialog(Text leftButtonText, Text rightButtonText, Consumer<ButtonComponent> leftButtonAction, Consumer<ButtonComponent> rightButtonAction, Collection<Text> messages) {
		this.uiAdapter.rootComponent.addOverlay(new DialogOverlayComponent(leftButtonText, rightButtonText, leftButtonAction, rightButtonAction, messages));
	}

	private void refreshPresets() {
		presetsTab.child().refresh();
		modulesTab.refresh();
		dataTab.refresh();
		this.uiAdapter.rootComponent.unlock();
	}

	public static class ManageTabLayout extends TabLayout {
		public ManageTabLayout(Collection<Component> leftComponents, Tab... tabs) {
			super(leftComponents, tabs);
			this.verticalAlignment(VerticalAlignment.CENTER);
			this.horizontalAlignment(HorizontalAlignment.CENTER);
		}
	}

	public class PresetsTabFlow extends VerticalFlowLayout {
		private String focusedPresetName;

		public final VerticalFlowLayout presetsFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
		public final ButtonComponent newPresetButton = Components.button(Text.translatable("screen.switchy.manage.presets.new"), b -> {
			focusedPresetName = "";
			refresh();
		});

		public PresetsTabFlow() {
			super(Sizing.fixed(200), Sizing.content());
			this.margins(Insets.horizontal(4));
			presetsFlow.gap(2);
			this.child(presetsFlow);
			this.child((Component) newPresetButton);
		}

		private void refresh() {
			presetsFlow.clearChildren();
			HorizontalFlowLayout focusedFlow = null;
			for (String name : presets.getPresets().keySet()) {
				HorizontalFlowLayout presetFlow = name.equals(focusedPresetName) ? getRenameFlow(name) : getPresetFlow(name);
				if (name.equals(focusedPresetName)) focusedFlow = presetFlow;
				presetsFlow.child(presetFlow);
			}
			if ("".equals(focusedPresetName)) {
				focusedFlow = getRenameFlow(null);
				presetsFlow.child(focusedFlow);
			}
			if (focusedFlow != null) presetsTab.scrollTo(focusedFlow);
		}

		private HorizontalFlowLayout getRenameFlow(@Nullable String presetName) {
			HorizontalFlowLayout renamePresetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			TextBoxComponent nameEntry = Components.textBox(Sizing.fill(53), (presetName != null) ? presetName : "newPreset");
			nameEntry.setTextPredicate(s -> s.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString));
			nameEntry.onChanged();
			ManageScreen.this.setInitialFocus(nameEntry);
			renamePresetFlow.child((Component) nameEntry);
			ButtonComponent confirmButton = Components.button(Text.translatable("screen.switchy.manage.presets.confirm"), (presetName != null) ? b -> {
				if (!presetName.equals(nameEntry.getText())) {
					if (presets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
						presets.renamePreset(presetName, nameEntry.getText());
						focusedPresetName = null;
						refresh();
						ManageScreen.this.uiAdapter.rootComponent.lock();
						SwitchyClientApi.renamePreset(presetName, nameEntry.getText(), SwitchyScreen::updatePresetScreens);
					}
				} else {
					focusedPresetName = null;
					refresh();
				}

			} : b -> {
				if (presets.getPresetNames().stream().noneMatch(s -> s.equalsIgnoreCase(nameEntry.getText()))) {
					presets.newPreset(nameEntry.getText());
					focusedPresetName = null;
					refresh();
					ManageScreen.this.uiAdapter.rootComponent.lock();
					SwitchyClientApi.newPreset(nameEntry.getText(), SwitchyScreen::updatePresetScreens);
				}
			});
			confirmButton.horizontalSizing(Sizing.fill(22));
			confirmButton.margins(Insets.vertical(1));
			ButtonComponent cancelButton = Components.button(Text.translatable("screen.switchy.manage.presets.cancel"), b -> {
				focusedPresetName = null;
				refresh();
			});
			cancelButton.horizontalSizing(Sizing.fill(22));
			renamePresetFlow.child((Component) confirmButton);
			renamePresetFlow.child((Component) cancelButton);
			renamePresetFlow.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
			renamePresetFlow.gap(2);
			return renamePresetFlow;
		}

		private HorizontalFlowLayout getPresetFlow(@Nullable String name) {
			HorizontalFlowLayout presetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent presetLabel = Components.label(Text.literal(name));
			presetLabel.horizontalSizing(Sizing.fill(54));
			ButtonComponent renameButton = Components.button(Text.translatable("screen.switchy.manage.presets.rename"), b -> {
				focusedPresetName = name;
				refresh();
			});
			renameButton.horizontalSizing(Sizing.fill(22));
			Consumer<ButtonComponent> deleteAction = b -> openDialog(Text.translatable("screen.switchy.manage.dialog.confirm"), Text.translatable("screen.switchy.manage.dialog.cancel"), okButton -> {
				presets.deletePreset(name);
				refresh();
				ManageScreen.this.uiAdapter.rootComponent.lock();
				SwitchyClientApi.deletePreset(name, SwitchyScreen::updatePresetScreens);
			}, cancel -> {
			}, List.of(Text.translatable("commands.switchy_client.delete.confirm", name), Text.translatable("screen.switchy.manage.messages.delete.warn"), Text.translatable("screen.switchy.manage.dialog.modules", presets.getEnabledModuleText())));
			ButtonComponent deleteButton = Components.button(Text.translatable("screen.switchy.manage.presets.delete"), deleteAction);
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

	}

	public class ModulesTab extends ModuleSelectorFlow {
		public ModulesTab() {
			super(160, Text.translatable("screen.switchy.manage.modules.disabled"), Text.translatable("screen.switchy.manage.modules.enabled"));
			this.margins(Insets.of(10));
		}

		public void refresh() {
			leftModulesFlow.clearChildren();
			rightModulesFlow.clearChildren();
			int labelSize = 100;
			leftModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
			}, true, Text.translatable("screen.switchy.manage.modules.disable"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));
			rightModulesFlow.child(getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
			}, true, Text.translatable("screen.switchy.manage.modules.disable"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));

			// Disabled Modules
			presets.getDisabledModules().forEach(module -> leftModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
				presets.enableModule(id);
				refresh();
				ManageScreen.this.uiAdapter.rootComponent.lock();
				SwitchyClientApi.enableModule(id, SwitchyScreen::updatePresetScreens);
			}, true, Text.translatable("screen.switchy.manage.modules.enable"), presets.getModuleInfo().get(module).descriptionWhenEnabled(), labelSize)));
			// Enabled Modules
			presets.getEnabledModules().forEach(module -> rightModulesFlow.child(getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> openDialog(Text.translatable("screen.switchy.manage.dialog.confirm"), Text.translatable("screen.switchy.manage.dialog.cancel"), okButton -> {
				presets.disableModule(id);
				refresh();
				ManageScreen.this.uiAdapter.rootComponent.lock();
				SwitchyClientApi.disableModule(id, SwitchyScreen::updatePresetScreens);
			}, cancel -> {
			}, List.of(Text.translatable("commands.switchy_client.disable.confirm", id.getPath()), Text.translatable("screen.switchy.manage.modules.disable.warn", presets.getModuleInfo().get(id).deletionWarning()))), true, Text.translatable("screen.switchy.manage.modules.disable"), presets.getModuleInfo().get(module).descriptionWhenDisabled(), labelSize)));
		}
	}

	public static class ModuleSelectorFlow extends HorizontalFlowLayout {
		public final ModulesFlow leftModulesFlow;
		public final ModulesFlow rightModulesFlow;

		public ModuleSelectorFlow(int vSize, Text leftText, Text rightText) {
			super(Sizing.content(), Sizing.content());
			this.gap(2);
			leftModulesFlow = new ModulesFlow();
			rightModulesFlow = new ModulesFlow();
			this.child(Containers.verticalFlow(Sizing.content(), Sizing.content()).child(Components.label(leftText)).child(new ModulesScroll(vSize, leftModulesFlow)).horizontalAlignment(HorizontalAlignment.CENTER));
			this.child(Containers.verticalFlow(Sizing.content(), Sizing.content()).child(Components.label(rightText)).child(new ModulesScroll(vSize, rightModulesFlow)).horizontalAlignment(HorizontalAlignment.CENTER));
		}

		public static HorizontalFlowLayout getModuleFlow(Identifier id, @Nullable Text labelTooltip, BiConsumer<ButtonComponent, Identifier> buttonAction, boolean enabled, Text buttonText, @Nullable Text buttonTooltip, int labelSize) {
			HorizontalFlowLayout moduleFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			LabelComponent moduleName = Components.label(Text.literal(id.getPath()));
			Text namespaceText = Text.literal(Feedback.guessModTitle(id.getNamespace())).setStyle(Feedback.FORMAT_INFO.getLeft());
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
	}

	public static class ModulesScroll extends ScrollContainer<VerticalFlowLayout> {
		protected ModulesScroll(int vSize, VerticalFlowLayout child) {
			super(ScrollDirection.VERTICAL, Sizing.content(), Sizing.fixed(vSize), child);
			this.surface(Surface.flat(0xFF141414).and(Surface.outline(0xFF202020)));
		}
	}

	public static class ModulesFlow extends VerticalFlowLayout {
		public ModulesFlow() {
			super(Sizing.content(), Sizing.content());
			this.gap(2);
		}
	}

	public class DataTabTabLayout extends TabLayout {
		public DataTabImportFlow importFlow;
		public DataTabExportFlow exportFlow;

		private DataTabTabLayout(DataTabImportFlow importFlow, DataTabExportFlow exportFlow) {
			super(List.of(),
				new TabLayout.Tab(Text.translatable("screen.switchy.manage.data.import"), importFlow),
				new TabLayout.Tab(Text.translatable("screen.switchy.manage.data.export"), exportFlow)
			);
			this.margins(Insets.top(6));
			this.contentPanel.verticalSizing(Sizing.fixed(174));
			this.contentPanel.padding(Insets.of(10));
			this.importFlow = importFlow;
			this.exportFlow = exportFlow;
		}

		public DataTabTabLayout() {
			this(new DataTabImportFlow(), new DataTabExportFlow());
		}

		public void refresh() {
			importFlow.updateDataMethod("");
			exportFlow.updateDataMethod("");
		}
	}

	public class DataTabImportFlow extends DataTabModeFlow {
		public DataTabImportFlow() {
			super(true);
			this.gap(2);
		}

		@Override
		protected void onAction() {
			openDialog(Text.translatable("screen.switchy.manage.dialog.confirm"), Text.translatable("screen.switchy.manage.dialog.cancel"), confirmButton -> {
				ManageScreen.this.uiAdapter.rootComponent.lock();
				SwitchyClientApi.importPresets(selectedFileNbt, availableModules, includedModules, SwitchyScreen::updatePresetScreens);
			}, cancelButton -> {
			}, List.of(
				Text.translatable("screen.switchy.manage.data.import.info",
					Feedback.literal(String.valueOf(selectedFileNbt.getCompound(SwitchyPresetsData.KEY_PRESETS).getKeys().size())),
					Feedback.literal(String.valueOf(includedModules.size()))), Text.translatable("screen.switchy.manage.dialog.presets",
					Feedback.getHighlightedListText(selectedFileNbt.getCompound(SwitchyPresetsData.KEY_PRESETS).getKeys().stream().sorted().toList(), List.of(new Pair<>(presets.getPresetNames()::contains, Formatting.DARK_RED)))),
				Text.translatable("screen.switchy.manage.data.import.collision"),
				Text.translatable("screen.switchy.manage.dialog.modules", Feedback.getIdListText(includedModules))
			));
		}

		@Override
		protected void onNbtSourceChange(NbtCompound selected) {
			super.onNbtSourceChange(selected);
			includedModules = selected.getList(SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).filter(id -> {
				SwitchyModuleInfo moduleInfo = presets.getModuleInfo().get(id);
				if (moduleInfo == null) return false;
				return moduleInfo.editable() == SwitchyModuleEditable.ALLOWED || moduleInfo.editable() == SwitchyModuleEditable.ALWAYS_ALLOWED;
			}).collect(Collectors.toList());
			availableModules = selected.getList(SwitchyPresetsData.KEY_PRESET_MODULE_ENABLED, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).filter(id -> {
				SwitchyModuleInfo moduleInfo = presets.getModuleInfo().get(id);
				if (moduleInfo == null) return true;
				return moduleInfo.editable() == SwitchyModuleEditable.OPERATOR || moduleInfo.editable() == SwitchyModuleEditable.NEVER;
			}).collect(Collectors.toList());
			refreshDataModulesFlow();
		}
	}

	public class DataTabExportFlow extends DataTabModeFlow {
		public DataTabExportFlow() {
			super(false);
			this.gap(2);
		}

		@Override
		protected void onAction() {
			openDialog(Text.translatable("screen.switchy.manage.dialog.confirm"), Text.translatable("screen.switchy.manage.dialog.cancel"), confirmButton -> {
				ManageScreen.this.uiAdapter.rootComponent.lock();
				SwitchyClientApi.exportPresetsToFile(availableModules, null, (feedback, file) -> SwitchyScreen.updatePresetScreens(feedback, presets));
			}, cancelButton -> {
			}, List.of(Text.translatable("commands.switchy_client.export.confirm", String.valueOf(includedModules.size()))));
		}

		@Override
		protected void onNbtSourceChange(NbtCompound selected) {
			super.onNbtSourceChange(selected);
			includedModules = new ArrayList<>(presets.getEnabledModules());
			availableModules = new ArrayList<>();
			refreshDataModulesFlow();
		}
	}

	public abstract class DataTabModeFlow extends VerticalFlowLayout {
		public Dropdown<String> methodDropdown = new Dropdown<>(ManageScreen.this.uiAdapter.rootComponent, Text.translatable("screen.switchy.manage.data.method"), this::updateDataMethod);
		public Dropdown<NbtCompound> fileDropdown = new Dropdown<>(ManageScreen.this.uiAdapter.rootComponent, Text.translatable("screen.switchy.manage.data.file"), this::onNbtSourceChange);
		public ModuleSelectorFlow moduleSelector = new ModuleSelectorFlow(80, Text.translatable("screen.switchy.manage.data.available"), Text.translatable("screen.switchy.manage.data.included"));
		public ButtonComponent actionButton;

		protected List<Identifier> includedModules = new ArrayList<>();
		protected List<Identifier> availableModules = new ArrayList<>();
		protected NbtCompound selectedFileNbt;
		protected final boolean isImporting;

		public DataTabModeFlow(boolean isImporting) {
			super(Sizing.content(), Sizing.content());
			this.margins(Insets.of(4));
			this.gap(6);
			moduleSelector.margins(Insets.vertical(4));
			this.isImporting = isImporting;
			Text fileText = Text.translatable("screen.switchy.manage.data.method.file");
			this.methodDropdown.comboBox.setOptions(Map.of(fileText, "file"), fileText);
			this.actionButton = Components.button(isImporting ? Text.translatable("screen.switchy.manage.data.import.action") : Text.translatable("screen.switchy.manage.data.export.action"), b -> this.onAction());
			this.child(methodDropdown);
			this.child(fileDropdown);
			this.child(moduleSelector);
			this.child((Component) actionButton);
		}

		protected abstract void onAction();

		protected void onNbtSourceChange(NbtCompound selected) {
			selectedFileNbt = selected;
		}

		public void updateDataMethod(String method) {
			Map<Text, NbtCompound> fileOptions = new HashMap<>();

			Text exportPrompt = Text.translatable("screen.switchy.manage.data.export.file.prompt");
			Text importPrompt = Text.translatable("screen.switchy.manage.data.import.file.prompt");

			if (isImporting) {
				SwitchyClientApi.getImportableFiles().forEach(file -> {
					try {
						NbtCompound nbt = NbtIo.readCompressed(file);
						nbt.putString("filename", FilenameUtils.getBaseName(file.getName()));

						String name = file.getName();
						String baseName = FileNameUtils.getBaseName(name);
						fileOptions.put(Text.of(baseName), nbt);
					} catch (IOException ignored) {
					}
				});
			} else {
				fileOptions.put(exportPrompt, new NbtCompound());
			}

			includedModules = isImporting ? new ArrayList<>() : new ArrayList<>(presets.getEnabledModules());
			availableModules = new ArrayList<>();

			fileDropdown.comboBox.setOptions(fileOptions, isImporting ? importPrompt : exportPrompt);
			refreshDataModulesFlow();
		}

		protected void refreshDataModulesFlow() {
			moduleSelector.leftModulesFlow.clearChildren();
			moduleSelector.rightModulesFlow.clearChildren();
			int labelSize = 100;
			moduleSelector.leftModulesFlow.child(ModuleSelectorFlow.getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
			}, false, Text.translatable("screen.switchy.manage.data.add"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));
			moduleSelector.rightModulesFlow.child(ModuleSelectorFlow.getModuleFlow(new Identifier("placeholder", "placeholder"), Text.literal(""), (b, i) -> {
			}, false, Text.translatable("screen.switchy.manage.data.remove"), Text.literal(""), labelSize).verticalSizing(Sizing.fixed(0)));

			if (isImporting) {
				// Available Modules
				List<Identifier> noPermissionModules = availableModules.stream().filter(m -> presets.getPermissionLevel() < 2 && presets.getModuleInfo().containsKey(m) && presets.getModuleInfo().get(m).editable() == SwitchyModuleEditable.OPERATOR).toList();
				List<Identifier> neverModules = availableModules.stream().filter(m -> presets.getModuleInfo().containsKey(m) && presets.getModuleInfo().get(m).editable() == SwitchyModuleEditable.NEVER).toList();
				List<Identifier> notInstalledModules = availableModules.stream().filter(m -> !presets.getModuleInfo().containsKey(m)).toList();
				List<Identifier> includableModules = availableModules.stream().filter(m -> !noPermissionModules.contains(m) && !neverModules.contains(m) && !notInstalledModules.contains(m)).toList();

				includableModules.forEach(module -> moduleSelector.leftModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow();
				}, true, Text.translatable("screen.switchy.manage.data.add"), Text.translatable("screen.switchy.manage.data.import.add.includable"), labelSize)));
				noPermissionModules.forEach(module -> moduleSelector.leftModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow();
				}, false, Text.translatable("screen.switchy.manage.data.add"), Text.translatable("screen.switchy.manage.data.import.add.permission"), labelSize)));
				neverModules.forEach(module -> moduleSelector.leftModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow();
				}, false, Text.translatable("screen.switchy.manage.data.add"), Text.translatable("screen.switchy.manage.data.import.add.never"), labelSize)));
				notInstalledModules.forEach(module -> moduleSelector.leftModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, null, (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow();
				}, false, Text.translatable("screen.switchy.manage.data.add"), Text.translatable("screen.switchy.manage.data.import.add.missing"), labelSize)));
				// Included Modules
				includedModules.forEach(module -> moduleSelector.rightModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					availableModules.add(module);
					includedModules.remove(module);
					refreshDataModulesFlow();
				}, true, Text.translatable("screen.switchy.manage.data.remove"), Text.translatable("screen.switchy.manage.data.import.remove"), labelSize)));
			} else {
				availableModules.forEach(module -> moduleSelector.leftModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					includedModules.add(module);
					availableModules.remove(module);
					refreshDataModulesFlow();
				}, true, Text.translatable("screen.switchy.manage.data.add"), Text.translatable("screen.switchy.manage.data.export.add"), labelSize)));
				includedModules.forEach(module -> moduleSelector.rightModulesFlow.child(ModuleSelectorFlow.getModuleFlow(module, presets.getModuleInfo().get(module).description(), (b, id) -> {
					availableModules.add(module);
					includedModules.remove(module);
					refreshDataModulesFlow();
				}, true, Text.translatable("screen.switchy.manage.data.remove"), Text.translatable("screen.switchy.manage.data.export.remove"), labelSize)));
			}
			actionButton.active(!includedModules.isEmpty());
		}
	}

	public static class Dropdown<T> extends HorizontalFlowLayout {
		public final ComboBox<T> comboBox;

		public Dropdown(FlowLayout contextParent, Text label, Consumer<T> onUpdate) {
			super(Sizing.content(), Sizing.content());
			this.verticalAlignment(VerticalAlignment.CENTER); // So label lines up with box.
			this.gap(4);
			this.comboBox = new ComboBox<>(Sizing.content(), contextParent, onUpdate);
			this.child(Components.label(label));
			this.child(comboBox);
		}
	}

	public static class ComboBox<T> extends DropdownComponent {
		public final Map<Text, T> options = new HashMap<>();
		private final Consumer<T> onUpdate;
		private final FlowLayout contextParent;

		public final DropdownComponent contextMenu;
		public final Button openMenuButton;

		public ComboBox(Sizing horizontalSizing, FlowLayout contextParent, Consumer<T> onUpdate) {
			super(horizontalSizing);
			this.contextParent = contextParent;
			this.onUpdate = onUpdate;
			this.contextMenu = Components.dropdown(Sizing.content());
			this.openMenuButton = new EditableButton(Text.of(""), b -> {
				if (!contextMenu.hasParent()) {
					contextMenu.positioning(Positioning.absolute(x(), y() + height()));
					contextParent.child(contextMenu);
				} else {
					contextParent.removeChild(contextMenu);
				}
			});
			this.entries.child(openMenuButton);
		}

		public void setOptions(Map<Text, T> options, Text selected) {
			this.options.clear();
			this.options.putAll(options);
			openMenuButton.text(selected);
			((FlowLayout) contextMenu.children().get(0)).clearChildren();
			this.options.keySet().forEach(t -> contextMenu.button(t, b -> {
				contextParent.removeChild(contextMenu);
				openMenuButton.text(t);
				this.onUpdate.accept(this.options.get(t));
			}));
		}

		public static class EditableButton extends Button {
			protected EditableButton(Text text, Consumer<DropdownComponent> onClick) {
				super(text, onClick);
			}
		}
	}
}

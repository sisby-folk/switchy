package folk.sisby.switchy.ui.screen;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.module.presets.SwitchyClientPreset;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import folk.sisby.switchy.ui.api.module.SwitchyUIModule;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The quick-switcher screen, populated by a {@link SwitchyClientPresets} object.
 * Allows the client to preview presets, and switch to a desired one.
 *
 * @author Sisby folk
 * @since 1.9.0
 */
public class SwitchScreen extends BaseOwoScreen<FlowLayout> implements SwitchyScreen {
	private static final List<Function<SwitchyClientPreset, Pair<Component, SwitchyUIPosition>>> basicComponents = new ArrayList<>();

	static {
		// Close on switch
		SwitchyClientEvents.SWITCH.register(event -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (Objects.equals(client.getSession().getPlayerUuid(), event.player()))
				client.execute(() -> {
					if (client.currentScreen instanceof SwitchScreen) client.setScreen(null);
				});
		});

		// Add base components
		registerBasicPresetComponent(clientPreset -> Pair.of(Components.label(Text.literal(clientPreset.getName())), SwitchyUIPosition.SIDE_LEFT));
	}

	/**
	 * Constructs an instance of the screen.
	 */
	public SwitchScreen() {
		super();
	}

	private ScrollContainer<VerticalFlowLayout> presetsScroll;
	private VerticalFlowLayout presetsFlow;
	private SwitchyClientPresets presets;
	private FlowLayout root;
	private VerticalFlowLayout loadingOverlay;


	/**
	 * Registers a component to display alongside every preset (e.g. the preset name) for addons.
	 * Modules should instead use {@link SwitchyUIModule}.
	 *
	 * @param componentFunction a function that can generate a positioned component to display with every preset.
	 */
	public static void registerBasicPresetComponent(Function<SwitchyClientPreset, Pair<Component, SwitchyUIPosition>> componentFunction) {
		basicComponents.add(componentFunction);
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		root = rootComponent;

		root.surface(Surface.VANILLA_TRANSLUCENT);
		root.horizontalAlignment(HorizontalAlignment.CENTER);
		root.verticalAlignment(VerticalAlignment.CENTER);
		root.gap(2);

		presetsFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
		presetsFlow.padding(Insets.of(6));
		presetsFlow.verticalAlignment(VerticalAlignment.CENTER);
		presetsFlow.horizontalAlignment(HorizontalAlignment.CENTER);
		presetsFlow.gap(4);

		presetsScroll = Containers.verticalScroll(Sizing.content(), Sizing.fill(80), presetsFlow);
		presetsScroll.surface(Surface.DARK_PANEL);
		presetsScroll.padding(Insets.of(4));

		HorizontalFlowLayout labelManageFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		labelManageFlow.verticalAlignment(VerticalAlignment.CENTER);
		labelManageFlow.gap(10);

		LabelComponent screenLabel = Components.label(Text.literal("Switch Preset"));


		ButtonComponent manageButton = Components.button(Text.literal("Manage"), b -> {
			ManageScreen managementScreen = new ManageScreen();
			client.setScreen(managementScreen);
			managementScreen.updatePresets(presets);
		});

		labelManageFlow.child(screenLabel);
		labelManageFlow.child(manageButton);

		root.child(labelManageFlow);
		root.child(presetsScroll);
		lockScreen();
	}

	void lockScreen() {
		if (loadingOverlay == null) {
			VerticalFlowLayout overlay = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
			overlay.alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM);
			overlay.positioning(Positioning.absolute(0, 0));
			overlay.child(Components.label(Text.of("Loading...")));
			overlay.mouseDown().subscribe((x, y, b) -> true); // eat all input
			loadingOverlay = overlay;
			root.child(loadingOverlay);
		}
	}

	@Override
	public void updatePresets(SwitchyClientPresets clientPresets) {
		presets = clientPresets;
		presetsFlow.clearChildren();

		// Process Preset Flows
		Component currentPresetComponent = null;
		for (SwitchyClientPreset preset : presets.getPresets().values()) {
			List<Pair<Component, SwitchyUIPosition>> componentList = new ArrayList<>(basicComponents.stream().map(fun -> fun.apply(preset)).toList());

			preset.getModules().forEach((id, module) -> {
				if (module instanceof SwitchyUIModule dm) {
					@Nullable Pair<Component, SwitchyUIPosition> component = dm.getPreviewComponent(preset.getName());
					if (component != null) {
						componentList.add(component);
					}
				}
			});

			// Main Horizontal Flow Panel
			HorizontalFlowLayout horizontalFlow = Containers.horizontalFlow(Sizing.fixed(400), Sizing.content());
			horizontalFlow.padding(Insets.vertical(4).withLeft(10).withRight(10));
			horizontalFlow.gap(2);
			horizontalFlow.verticalAlignment(VerticalAlignment.CENTER);
			horizontalFlow.horizontalAlignment(HorizontalAlignment.CENTER);
			if (preset.getName().equals(presets.getCurrentPresetName())) {
				horizontalFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.BLUE.argb())));
				currentPresetComponent = horizontalFlow;
			} else {
				horizontalFlow.surface(Surface.DARK_PANEL);
				horizontalFlow.mouseEnter().subscribe(() -> horizontalFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.WHITE.argb()))));
				horizontalFlow.mouseLeave().subscribe(() -> horizontalFlow.surface(Surface.DARK_PANEL));
				horizontalFlow.mouseDown().subscribe((x, y, button) -> {
					SwitchyClientApi.switchCurrentPreset(preset.getName(), SwitchyScreen::updatePresetScreens);
					return true;
				});
			}

			// Left Side Elements
			List<Component> sideLeftComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.SIDE_LEFT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!sideLeftComponents.isEmpty()) horizontalFlow.children(sideLeftComponents);

			// Main Elements
			HorizontalFlowLayout leftRightFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			leftRightFlow.margins(Insets.horizontal(6));
			leftRightFlow.gap(4);

			List<Component> leftComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.LEFT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!leftComponents.isEmpty()) {
				VerticalFlowLayout leftAlignedFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
				leftAlignedFlow.horizontalAlignment(HorizontalAlignment.LEFT);
				leftAlignedFlow.gap(2);
				leftAlignedFlow.children(leftComponents);
				leftRightFlow.child(leftAlignedFlow);
			}

			List<Component> rightComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.RIGHT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!rightComponents.isEmpty()) {
				VerticalFlowLayout rightAlignedFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
				rightAlignedFlow.horizontalAlignment(HorizontalAlignment.RIGHT);
				rightAlignedFlow.gap(2);
				rightAlignedFlow.children(rightComponents);
				leftRightFlow.child(rightAlignedFlow);
			}

			if (!rightComponents.isEmpty() || !leftComponents.isEmpty()) horizontalFlow.child(leftRightFlow);

			// Right Side Elements
			List<Component> sideRightComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.SIDE_RIGHT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!sideRightComponents.isEmpty()) horizontalFlow.children(sideRightComponents);

			presetsFlow.child(horizontalFlow);
		}

		if (currentPresetComponent != null) presetsScroll.scrollTo(currentPresetComponent);
		// Clear locked state
		if (loadingOverlay != null) {
			root.removeChild(loadingOverlay);
			loadingOverlay = null;
		}
	}
}

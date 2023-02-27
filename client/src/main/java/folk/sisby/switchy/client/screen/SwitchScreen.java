package folk.sisby.switchy.client.screen;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets;
import folk.sisby.switchy.client.SwitchyClientNetworking;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Sisby folk
 * @since 1.9.0
 * The quick-switcher screen, populated by a {@link SwitchyDisplayPresets} object.
 * Allows the client to preview presets, and switch to a desired one.
 */
public class SwitchScreen extends BaseOwoScreen<FlowLayout> {
	private static final List<Function<SwitchyDisplayPreset, Pair<Component, SwitchySwitchScreenPosition>>> basicComponents = new ArrayList<>();

	static {
		// Close on switch
		SwitchyClientEvents.SWITCH.register(event -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (Objects.equals(client.getSession().getPlayerUuid(), event.player))
				client.execute(() -> {
					if (client.currentScreen instanceof SwitchScreen) client.setScreen(null);
				});
		});

		// Add base components
		registerBasicPresetComponent(displayPreset -> Pair.of(Components.label(Text.literal(displayPreset.getName())), SwitchySwitchScreenPosition.SIDE_LEFT));
	}

	final SwitchyDisplayPresets displayPresets;

	/**
	 * @param displayPresets a display presets object to preview and switch with
	 */
	public SwitchScreen(SwitchyDisplayPresets displayPresets) {
		super();
		this.displayPresets = displayPresets;
	}

	/**
	 * @param componentFunction a function that can generate a positioned component to display with every preset
	 *                          Registers a component to display alongside every preset (e.g. the preset name) for addons.
	 *                          Modules should instead use {@link folk.sisby.switchy.client.api.module.SwitchyDisplayModule}
	 */
	public static void registerBasicPresetComponent(Function<SwitchyDisplayPreset, Pair<Component, SwitchySwitchScreenPosition>> componentFunction) {
		basicComponents.add(componentFunction);
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
		rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
		rootComponent.verticalAlignment(VerticalAlignment.CENTER);
		rootComponent.gap(2);

		VerticalFlowLayout presetsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());
		presetsLayout.padding(Insets.of(6));
		presetsLayout.verticalAlignment(VerticalAlignment.CENTER);
		presetsLayout.horizontalAlignment(HorizontalAlignment.CENTER);
		presetsLayout.gap(4);

		ScrollContainer<VerticalFlowLayout> presetsScroll = Containers.verticalScroll(Sizing.content(), Sizing.fill(80), presetsLayout);
		presetsScroll.surface(Surface.DARK_PANEL);
		presetsScroll.padding(Insets.of(4));

		LabelComponent screenLabel = Components.label(Text.literal("Switchy Presets"));

		rootComponent.child(screenLabel);
		rootComponent.child(presetsScroll);

		// Process Preset Flows
		Component currentPresetComponent = null;
		for (SwitchyDisplayPreset preset : displayPresets.getPresets().values()) {
			List<Pair<Component, SwitchySwitchScreenPosition>> componentList = new ArrayList<>(basicComponents.stream().map(fun -> fun.apply(preset)).toList());
			componentList.addAll(preset.getDisplayComponents().values());

			// Main Horizontal Flow Panel
			HorizontalFlowLayout horizontalFlow = Containers.horizontalFlow(Sizing.fixed(400), Sizing.content());
			horizontalFlow.padding(Insets.vertical(4).withLeft(10).withRight(10));
			horizontalFlow.gap(2);
			horizontalFlow.verticalAlignment(VerticalAlignment.CENTER);
			horizontalFlow.horizontalAlignment(HorizontalAlignment.CENTER);
			if (preset.getName().equals(displayPresets.getCurrentPresetName())) {
				horizontalFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.BLUE.argb())));
				currentPresetComponent = horizontalFlow;
			} else {
				horizontalFlow.surface(Surface.DARK_PANEL);
				horizontalFlow.mouseEnter().subscribe(() -> horizontalFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.WHITE.argb()))));
				horizontalFlow.mouseLeave().subscribe(() -> horizontalFlow.surface(Surface.DARK_PANEL));
				horizontalFlow.mouseDown().subscribe((x, y, button) -> {
					SwitchyClientNetworking.switchCurrentPreset(preset.getName());
					return true;
				});
			}

			// Left Side Elements
			List<Component> sideLeftComponents = componentList.stream().filter(p -> p.getSecond() == SwitchySwitchScreenPosition.SIDE_LEFT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!sideLeftComponents.isEmpty()) horizontalFlow.children(sideLeftComponents);

			// Main Elements
			HorizontalFlowLayout leftRightFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			leftRightFlow.margins(Insets.horizontal(6));
			leftRightFlow.gap(4);

			List<Component> leftComponents = componentList.stream().filter(p -> p.getSecond() == SwitchySwitchScreenPosition.LEFT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!leftComponents.isEmpty()) {
				VerticalFlowLayout leftAlignedFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
				leftAlignedFlow.horizontalAlignment(HorizontalAlignment.LEFT);
				leftAlignedFlow.gap(2);
				leftAlignedFlow.children(leftComponents);
				leftRightFlow.child(leftAlignedFlow);
			}

			List<Component> rightComponents = componentList.stream().filter(p -> p.getSecond() == SwitchySwitchScreenPosition.RIGHT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!rightComponents.isEmpty()) {
				VerticalFlowLayout rightAlignedFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
				rightAlignedFlow.horizontalAlignment(HorizontalAlignment.RIGHT);
				rightAlignedFlow.gap(2);
				rightAlignedFlow.children(rightComponents);
				leftRightFlow.child(rightAlignedFlow);
			}

			if (!rightComponents.isEmpty() || !leftComponents.isEmpty()) horizontalFlow.child(leftRightFlow);

			// Right Side Elements
			List<Component> sideRightComponents = componentList.stream().filter(p -> p.getSecond() == SwitchySwitchScreenPosition.SIDE_RIGHT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!sideRightComponents.isEmpty()) horizontalFlow.children(sideRightComponents);

			presetsLayout.child(horizontalFlow);
		}

		if (currentPresetComponent != null) presetsScroll.scrollTo(currentPresetComponent);
	}
}

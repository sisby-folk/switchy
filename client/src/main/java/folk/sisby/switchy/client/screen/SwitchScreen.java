package folk.sisby.switchy.client.screen;

import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.SwitchyClientNetworking;
import folk.sisby.switchy.client.api.SwitchyEventsClient;
import folk.sisby.switchy.client.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.client.presets.SwitchyDisplayPresets;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class SwitchScreen extends BaseOwoScreen<FlowLayout> {
	public final SwitchyDisplayPresets displayPresets;
	public enum ComponentPosition {
		SIDE_LEFT,
		LEFT,
		RIGHT,
		SIDE_RIGHT
	}
	private static final Map<Identifier, Function<SwitchyDisplayPreset, Component>> sideLeftComponents = new LinkedHashMap<>();
	private static final Map<Identifier, Function<SwitchyDisplayPreset, Component>> leftComponents = new LinkedHashMap<>();
	private static final Map<Identifier, Function<SwitchyDisplayPreset, Component>> rightComponents = new LinkedHashMap<>();
	private static final Map<Identifier, Function<SwitchyDisplayPreset, Component>> sideRightComponents = new LinkedHashMap<>();

	public static void registerPresetDisplayComponent(Identifier id, ComponentPosition pos, Function<SwitchyDisplayPreset, Component> componentFunction) {
		switch (pos) {
			case SIDE_LEFT -> sideLeftComponents.put(id, componentFunction);
			case LEFT -> leftComponents.put(id, componentFunction);
			case RIGHT -> rightComponents.put(id, componentFunction);
			case SIDE_RIGHT -> sideRightComponents.put(id, componentFunction);
		}
	}


	public SwitchScreen(SwitchyDisplayPresets displayPresets) {
		super();
		this.displayPresets = displayPresets;
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	private Component generatePresetComponent(SwitchyDisplayPreset preset) {
		// Main Horizontal Flow Panel
		HorizontalFlowLayout horizontalFLow = Containers.horizontalFlow(Sizing.fixed(400), Sizing.content());
		horizontalFLow.padding(Insets.vertical(4).withLeft(10).withRight(10));
		horizontalFLow.gap(2);
		horizontalFLow.surface(Surface.DARK_PANEL);
		horizontalFLow.verticalAlignment(VerticalAlignment.CENTER);
		horizontalFLow.horizontalAlignment(HorizontalAlignment.CENTER);
		horizontalFLow.mouseEnter().subscribe(() -> horizontalFLow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.WHITE.argb()))));
		horizontalFLow.mouseLeave().subscribe(() -> horizontalFLow.surface(Surface.DARK_PANEL));
		horizontalFLow.mouseDown().subscribe((x, y, button) -> {
			SwitchyClientNetworking.sendSwitch(preset.presetName);
			return true;
		});

		// Left Side Elements
		horizontalFLow.children(sideLeftComponents.values().stream().map((fun) -> fun.apply(preset)).filter(Objects::nonNull).toList());

		// Main Elements
		HorizontalFlowLayout leftRightFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
		leftRightFlow.gap(4);

		VerticalFlowLayout leftAlignedFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
		leftAlignedFlow.horizontalAlignment(HorizontalAlignment.LEFT);
		leftAlignedFlow.gap(2);
		leftAlignedFlow.children(leftComponents.values().stream().map((fun) -> fun.apply(preset)).filter(Objects::nonNull).toList());
		leftRightFlow.child(leftAlignedFlow);

		VerticalFlowLayout rightAlignedFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
		rightAlignedFlow.horizontalAlignment(HorizontalAlignment.RIGHT);
		rightAlignedFlow.gap(2);
		rightAlignedFlow.children(rightComponents.values().stream().map((fun) -> fun.apply(preset)).filter(Objects::nonNull).toList());
		leftRightFlow.child(rightAlignedFlow);

		horizontalFLow.child(leftRightFlow);

		// Right Side Elements
		horizontalFLow.children(sideRightComponents.values().stream().map((fun) -> fun.apply(preset)).filter(Objects::nonNull).toList());

		return horizontalFLow;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
		rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
		rootComponent.verticalAlignment(VerticalAlignment.CENTER);

		List<Component> presetFlows = new ArrayList<>(displayPresets.presets.values().stream().map(this::generatePresetComponent).toList());

		VerticalFlowLayout presetsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());
		presetsLayout.padding(Insets.of(6));
		presetsLayout.verticalAlignment(VerticalAlignment.CENTER);
		presetsLayout.horizontalAlignment(HorizontalAlignment.CENTER);
		presetsLayout.gap(4);
		presetsLayout.children(presetFlows);

		ScrollContainer<VerticalFlowLayout> presetsScroll = Containers.verticalScroll(Sizing.content(), Sizing.fill(80), presetsLayout);
		presetsScroll.surface(Surface.DARK_PANEL);
		presetsScroll.padding(Insets.of(4));

		LabelComponent screenLabel = Components.label(Text.literal("Switchy Presets"));

		VerticalFlowLayout screenLabelFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
		screenLabelFlow.horizontalAlignment(HorizontalAlignment.CENTER);
		screenLabelFlow.gap(2);
		screenLabelFlow.children(List.of(screenLabel, presetsScroll));

		rootComponent.child(screenLabelFlow);
	}

	static {
		// Close on switch
		SwitchyEventsClient.registerSwitchListener(new Identifier(SwitchyClient.ID, "quick_switch_close"), (event) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (Objects.equals(client.getSession().getPlayerUuid(), event.player))
				client.execute(() -> {
					if (client.currentScreen instanceof SwitchScreen) client.setScreen(null);
				});
		});

		// Add base components
		registerPresetDisplayComponent(new Identifier(SwitchyClient.ID, "preset_name"), ComponentPosition.LEFT, displayPreset -> Components.label(Text.literal(displayPreset.presetName)));
	}
}

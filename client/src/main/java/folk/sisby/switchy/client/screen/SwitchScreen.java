package folk.sisby.switchy.client.screen;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchySwitchScreenPosition;
import folk.sisby.switchy.client.SwitchyClientNetworking;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.presets.SwitchyDisplayPresets;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class SwitchScreen extends BaseOwoScreen<FlowLayout> {
	public final SwitchyDisplayPresets displayPresets;

	private static final List<Function<SwitchyDisplayPreset, Pair<Component, SwitchySwitchScreenPosition>>> componentFunctions = new ArrayList<>();

	public static void registerBasicPresetComponent(Function<SwitchyDisplayPreset, Pair<Component, SwitchySwitchScreenPosition>> componentFunction) {
		componentFunctions.add(componentFunction);
	}

	public SwitchScreen(SwitchyDisplayPresets displayPresets) {
		super();
		this.displayPresets = displayPresets;
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	private Component generatePresetComponent(SwitchyDisplayPreset preset, boolean currentPreset) {
		List<Pair<Component, SwitchySwitchScreenPosition>> componentList = new ArrayList<>(componentFunctions.stream().map(fun -> fun.apply(preset)).toList());
		componentList.addAll(preset.getDisplayComponents().values());

		// Main Horizontal Flow Panel
		HorizontalFlowLayout horizontalFlow = Containers.horizontalFlow(Sizing.fixed(400), Sizing.content());
		horizontalFlow.padding(Insets.vertical(4).withLeft(10).withRight(10));
		horizontalFlow.gap(2);
		horizontalFlow.verticalAlignment(VerticalAlignment.CENTER);
		horizontalFlow.horizontalAlignment(HorizontalAlignment.CENTER);
		if (currentPreset) {
			horizontalFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.BLUE.argb())));
		} else {
			horizontalFlow.surface(Surface.DARK_PANEL);
			horizontalFlow.mouseEnter().subscribe(() -> horizontalFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.WHITE.argb()))));
			horizontalFlow.mouseLeave().subscribe(() -> horizontalFlow.surface(Surface.DARK_PANEL));
			horizontalFlow.mouseDown().subscribe((x, y, button) -> {
				SwitchyClientNetworking.sendSwitch(preset.name);
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

		return horizontalFlow;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
		rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
		rootComponent.verticalAlignment(VerticalAlignment.CENTER);
		rootComponent.gap(2);

		List<Component> presetFlows = displayPresets.presets.values().stream().map(preset -> generatePresetComponent(preset, Objects.equals(preset.name, displayPresets.currentPreset.toString()))).toList();

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
		rootComponent.child(screenLabel);
		rootComponent.child(presetsScroll);
	}

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
		registerBasicPresetComponent(displayPreset -> Pair.of(Components.label(Text.literal(displayPreset.name)), SwitchySwitchScreenPosition.SIDE_LEFT));
	}
}

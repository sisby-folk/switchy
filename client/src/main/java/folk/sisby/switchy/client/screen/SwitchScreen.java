package folk.sisby.switchy.client.screen;

import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.SwitchyClientNetworking;
import folk.sisby.switchy.client.api.SwitchyEventsClient;
import folk.sisby.switchy.client.presets.SwitchyDisplayPreset;
import folk.sisby.switchy.client.presets.SwitchyDisplayPresets;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.HorizontalFlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class SwitchScreen extends BaseOwoScreen<FlowLayout> {
	public final SwitchyDisplayPresets displayPresets;
	private static final Map<Identifier, Function<SwitchyDisplayPreset, Component>> components = new HashMap<>();

	public static void registerPresetDisplayComponent(Identifier id, Function<SwitchyDisplayPreset, Component> componentFunction) {
		components.put(id, componentFunction);
	}


	public SwitchScreen(SwitchyDisplayPresets displayPresets) {
		super();
		this.displayPresets = displayPresets;
	}

	@Override
	protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, Containers::verticalFlow);
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		rootComponent
				.surface(Surface.VANILLA_TRANSLUCENT)
				.horizontalAlignment(HorizontalAlignment.CENTER)
				.verticalAlignment(VerticalAlignment.CENTER);

		List<Component> presets = new ArrayList<>();
		displayPresets.presets.forEach((name, preset) -> {
			HorizontalFlowLayout presetFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
			presetFlow.children(components.values().stream().map((fun) -> fun.apply(preset)).toList());
			presetFlow.padding(Insets.of(10));
			presetFlow.margins(Insets.vertical(2));
			presetFlow.surface(Surface.DARK_PANEL);
			presetFlow.mouseDown().subscribe((x, y, button) -> {
				SwitchyClientNetworking.sendSwitch(name);
				return true;
			});
			presetFlow.mouseEnter().subscribe(() -> {
				presetFlow.surface(Surface.DARK_PANEL.and(Surface.outline(Color.WHITE.argb())));
			});
			presetFlow.mouseLeave().subscribe(() -> {
				presetFlow.surface(Surface.DARK_PANEL);
			});
			presets.add(presetFlow);
		});

		rootComponent.child(
				Containers.verticalScroll(Sizing.content(), Sizing.fill(80),
						Containers.verticalFlow(Sizing.content(), Sizing.content())
								.children(presets)
								.padding(Insets.of(10))
								.verticalAlignment(VerticalAlignment.CENTER)
								.horizontalAlignment(HorizontalAlignment.CENTER)
				).surface(Surface.DARK_PANEL)
		);
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
		registerPresetDisplayComponent(new Identifier(SwitchyClient.ID, "preset_name"), displayPreset -> Components.label(Text.literal(displayPreset.presetName)));
	}
}

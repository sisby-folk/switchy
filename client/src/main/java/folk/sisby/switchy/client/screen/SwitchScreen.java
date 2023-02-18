package folk.sisby.switchy.client.screen;

import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.SwitchyClientNetworking;
import folk.sisby.switchy.client.api.SwitchyEventsClient;
import folk.sisby.switchy.client.presets.SwitchyDisplayPresets;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SwitchScreen extends BaseOwoScreen<FlowLayout> {
	public final SwitchyDisplayPresets displayPresets;


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

		List<Component> buttons = new ArrayList<>();
		displayPresets.presets.keySet().forEach(name -> buttons.add(Components.button(Text.literal(name), button -> SwitchyClientNetworking.sendSwitch(name)).margins(Insets.vertical(5))));

		rootComponent.child(
				Containers.verticalScroll(Sizing.content(), Sizing.fill(80),
						Containers.verticalFlow(Sizing.content(), Sizing.content())
								.children(buttons)
								.padding(Insets.of(10))
								.verticalAlignment(VerticalAlignment.CENTER)
								.horizontalAlignment(HorizontalAlignment.CENTER)
				).surface(Surface.DARK_PANEL)
		);
	}

	static {
		SwitchyEventsClient.registerSwitchListener(new Identifier(SwitchyClient.ID, "quick_switch_close"), (event) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (Objects.equals(client.getSession().getPlayerUuid(), event.player))
				client.execute(() -> {
					if (client.currentScreen instanceof SwitchScreen) client.setScreen(null);
				});
		});
	}
}

package folk.sisby.switchy.ui.screen;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.module.presets.SwitchyClientPreset;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.client.api.SwitchyClientApi;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import folk.sisby.switchy.ui.api.module.SwitchyUIModule;
import folk.sisby.switchy.ui.component.LockableFlowLayout;
import folk.sisby.switchy.util.Feedback;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.HorizontalFlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.VerticalFlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
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
public class SwitchScreen extends BaseOwoScreen<LockableFlowLayout> implements SwitchyScreen {
	private static final List<Function<SwitchyClientPreset, Pair<Component, SwitchyUIPosition>>> basicComponents = new ArrayList<>();

	static {
		// Close Screen on Switch
		SwitchyClientEvents.SWITCH.register(event -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (Objects.equals(client.getSession().getUuidOrNull(), event.player()))
				client.execute(() -> {
					if (client.currentScreen instanceof SwitchScreen) client.setScreen(null);
				});
		});

		// Add Preset Name
		registerBasicPresetComponent(clientPreset -> Pair.of(Components.label(Feedback.literal(clientPreset.getName())), SwitchyUIPosition.SIDE_LEFT));
	}

	private SwitcherScrollContainer presetsScroll;
	private SwitchyClientPresets presets;

	/**
	 * Constructs an instance of the screen.
	 */
	public SwitchScreen() {
		super();
	}

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
	protected @NotNull OwoUIAdapter<LockableFlowLayout> createAdapter() {
		return OwoUIAdapter.create(this, LockableFlowLayout::new);
	}

	@Override
	protected void build(LockableFlowLayout root) {
		root.child(new SwitcherTitleFlow());
		presetsScroll = new SwitcherScrollContainer();
		root.child(presetsScroll);
		root.lock();
	}

	protected List<Pair<Component, SwitchyUIPosition>> generatePreviewComponents(SwitchyClientPreset preset) {
		List<Pair<Component, SwitchyUIPosition>> outlist = new ArrayList<>(basicComponents.stream().map(fun -> fun.apply(preset)).toList());
		preset.getModules(SwitchyUIModule.class).values().stream().map(m -> m.getPreviewComponent(preset.getName())).filter(Objects::nonNull).forEach(outlist::add);
		return outlist;
	}

	@Override
	public void updatePresets(SwitchyClientPresets clientPresets) {
		presets = clientPresets;
		presetsScroll.child().clearChildren();

		// Process Preset Flows
		Component currentPresetComponent = null;
		for (SwitchyClientPreset preset : presets.getPresets().values()) {
			boolean currentPreset = preset.getName().equals(presets.getCurrentPresetName());
			PresetFlow presetFlow = new PresetFlow(preset.getName(), currentPreset);
			if (currentPreset) currentPresetComponent = presetFlow;

			// Generate Component List
			List<Pair<Component, SwitchyUIPosition>> componentList = generatePreviewComponents(preset);

			// Left Side Elements
			List<Component> sideLeftComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.SIDE_LEFT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!sideLeftComponents.isEmpty()) presetFlow.children(sideLeftComponents);

			// Left Aligned Elements
			List<Component> leftComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.LEFT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!leftComponents.isEmpty()) presetFlow.child(new PreviewLeftVerticalFlow(leftComponents));

			// Grid Right Elements
			List<Component> gridRightComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.GRID_RIGHT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!gridRightComponents.isEmpty()) presetFlow.child(new PreviewRightGridLayout(gridRightComponents));

			// Right Side Elements
			List<Component> sideRightComponents = componentList.stream().filter(p -> p.getSecond() == SwitchyUIPosition.SIDE_RIGHT).map(Pair::getFirst).filter(Objects::nonNull).toList();
			if (!sideRightComponents.isEmpty()) presetFlow.children(sideRightComponents);

			presetsScroll.child().child(presetFlow);
		}

		if (currentPresetComponent != null) presetsScroll.scrollTo(currentPresetComponent);
		this.uiAdapter.rootComponent.unlock();
	}

	public class SwitcherTitleFlow extends HorizontalFlowLayout {
		protected SwitcherTitleFlow() {
			super(Sizing.content(), Sizing.content());
			this.verticalAlignment(VerticalAlignment.CENTER);
			this.gap(10);

			LabelComponent screenLabel = Components.label(Feedback.translatable("screen.switchy.switch.title"));

			ButtonComponent manageButton = Components.button(Feedback.translatable("screen.switchy.switch.manage"), b -> {
				ManageScreen managementScreen = new ManageScreen();
				if (client != null) client.setScreen(managementScreen);
				managementScreen.updatePresets(presets);
			});

			this.child(screenLabel);
			this.child((Component) manageButton);
		}
	}

	public static class SwitcherScrollContainer extends ScrollContainer<SwitcherFlow> {
		protected SwitcherScrollContainer() {
			super(ScrollDirection.VERTICAL, Sizing.content(), Sizing.fill(80), new SwitcherFlow());
			this.surface(Surface.DARK_PANEL);
			this.padding(Insets.of(4));
		}
	}

	public static class SwitcherFlow extends VerticalFlowLayout {
		protected SwitcherFlow() {
			super(Sizing.content(), Sizing.content());
			this.padding(Insets.of(6));
			this.verticalAlignment(VerticalAlignment.CENTER);
			this.horizontalAlignment(HorizontalAlignment.CENTER);
			this.gap(4);
		}
	}

	public static class PresetFlow extends HorizontalFlowLayout {
		protected PresetFlow(String presetName, boolean current) {
			super(Sizing.fixed(400), Sizing.content());
			this.padding(Insets.vertical(4).withLeft(10).withRight(10));
			this.gap(4);
			this.verticalAlignment(VerticalAlignment.CENTER);
			this.horizontalAlignment(HorizontalAlignment.CENTER);
			if (current) {
				this.surface(Surface.DARK_PANEL.and(Surface.outline(Color.BLUE.argb())));
			} else {
				this.surface(Surface.DARK_PANEL);
				this.mouseEnter().subscribe(() -> this.surface(Surface.DARK_PANEL.and(Surface.outline(Color.WHITE.argb()))));
				this.mouseLeave().subscribe(() -> this.surface(Surface.DARK_PANEL));
				this.mouseDown().subscribe((x, y, button) -> {
					SwitchyClientApi.switchCurrentPreset(presetName, SwitchyScreen::updatePresetScreens);
					return true;
				});
			}
		}
	}

	public static class PreviewLeftVerticalFlow extends VerticalFlowLayout {
		protected PreviewLeftVerticalFlow(Collection<Component> children) {
			super(Sizing.content(), Sizing.content());
			this.horizontalAlignment(HorizontalAlignment.LEFT);
			this.gap(2);
			this.children(children);
		}
	}

	public static class PreviewRightGridLayout extends GridLayout {
		protected PreviewRightGridLayout(Collection<Component> children) {
			super(Sizing.content(), Sizing.content(), (int) Math.ceil(Math.sqrt(children.size())), (int) Math.ceil(Math.sqrt(children.size())));
			int i = 0;
			for (Component child : children) {
				this.child(child, i % rows, i / rows);
				i++;
			}
		}
	}
}

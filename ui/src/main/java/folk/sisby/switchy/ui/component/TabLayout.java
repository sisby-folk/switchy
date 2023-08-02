package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TabLayout extends FlowLayout {
	protected List<TabButton> tabButtons = new ArrayList<>();
	protected List<Component> tabContents = new ArrayList<>();
	protected TabContentPanel contentPanel = new TabContentPanel();

	public TabLayout(Collection<Component> leftComponents, Tab... tabs) {
		super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

		Arrays.stream(tabs).forEach(t -> tabButtons.add(new TabButton(t.buttonText, t.tabContents)));
		Arrays.stream(tabs).forEach(t -> tabContents.add(t.tabContents));
		List<Component> buttons = new ArrayList<>(leftComponents);
		buttons.addAll(tabButtons);

		this.child(new TabButtonFlow(buttons));
		this.child(contentPanel);

		tabButtons.get(0).active(false);
		contentPanel.child(tabContents.get(0));
	}

	public record Tab(Text buttonText, Component tabContents) {}

	public void swapTabs(ButtonComponent tabButton, Component tabComponent) {
		contentPanel.clearChildren();
		contentPanel.child(tabComponent);
		tabButtons.forEach(b2 -> b2.active(true));
		tabButton.active(false);
	}

	public class TabButton extends ButtonComponent {
		public TabButton(Text message, Component tabComponent) {
			super(message, b -> TabLayout.this.swapTabs(b, tabComponent));
		}
	}

	public static class TabContentPanel extends FlowLayout {
		protected TabContentPanel() {
			super(Sizing.content(), Sizing.fixed(200), Algorithm.VERTICAL);
			this.verticalAlignment(VerticalAlignment.CENTER);
			this.horizontalAlignment(HorizontalAlignment.CENTER);
			this.surface(Surface.DARK_PANEL);
		}
	}

	public static class TabButtonFlow extends FlowLayout {
		protected TabButtonFlow(Collection<Component> children) {
			super(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL);
			this.verticalAlignment(VerticalAlignment.BOTTOM);
			this.margins(Insets.horizontal(8));
			this.children(children);
		}
	}
}

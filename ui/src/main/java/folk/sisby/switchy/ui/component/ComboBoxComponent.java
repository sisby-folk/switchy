package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ComboBoxComponent<T> extends DropdownComponent {
	public final Map<Text, T> options = new HashMap<>();
	private final Consumer<T> onUpdate;
	private final FlowLayout contextParent;

	public final DropdownComponent contextMenu;
	public final Button openMenuButton;

	public ComboBoxComponent(Sizing horizontalSizing, FlowLayout contextParent, Consumer<T> onUpdate) {
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

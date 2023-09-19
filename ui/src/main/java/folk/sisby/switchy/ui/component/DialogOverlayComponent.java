package folk.sisby.switchy.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.HorizontalFlowLayout;
import io.wispforest.owo.ui.container.VerticalFlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.function.Consumer;

public class DialogOverlayComponent extends OverlayComponent<VerticalFlowLayout> {
	public final VerticalFlowLayout messageFlow = Containers.verticalFlow(Sizing.content(), Sizing.content());
	public final HorizontalFlowLayout actionsFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());

	public DialogOverlayComponent(Text leftButtonText, Text rightButtonText, Consumer<ButtonComponent> leftButtonAction, Consumer<ButtonComponent> rightButtonAction, Collection<Text> messages) {
		super(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, Containers.verticalFlow(Sizing.fixed(200), Sizing.content()));
		child.surface(Surface.DARK_PANEL);
		child.padding(Insets.of(10));
		child.gap(4);
		messageFlow.gap(2);
		actionsFlow.gap(4);
		messages.forEach(t -> messageFlow.child(Components.label(t).horizontalSizing(Sizing.fill(90))));
		child.child(messageFlow);
		actionsFlow.child((Component) Components.button(leftButtonText, leftB -> {
			dismiss();
			leftButtonAction.accept(leftB);
		}));
		actionsFlow.child((Component) Components.button(rightButtonText, rightB -> {
			dismiss();
			rightButtonAction.accept(rightB);
		}));
		child.child(actionsFlow);
	}
}

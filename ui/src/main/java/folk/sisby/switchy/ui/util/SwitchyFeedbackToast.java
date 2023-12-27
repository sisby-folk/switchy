package folk.sisby.switchy.ui.util;

import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.api.SwitchyFeedbackStatus;
import folk.sisby.switchy.util.Feedback;
import io.wispforest.owo.ops.TextOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A toast capable of displaying {@link SwitchyFeedback}.
 * Borrows from {@code io.wispforest.owo.ui.util.UIErrorToast}.
 *
 * @author Sisby Folk
 * @since 2.0.0
 */
public class SwitchyFeedbackToast implements Toast {
	private static final Map<SwitchyFeedbackStatus, Integer> colours = Map.of(
			SwitchyFeedbackStatus.SUCCESS, 0xA700FF00,
			SwitchyFeedbackStatus.INVALID, 0xA7AAAA00,
			SwitchyFeedbackStatus.FAIL, 0xA7FF0000
	);
	private final SwitchyFeedbackStatus status;
	private final List<OrderedText> textLines;
	private final TextRenderer textRenderer;
	private final int duration;
	private final int width;

	private SwitchyFeedbackToast(SwitchyFeedback feedback, int duration) {
		this.duration = duration;
		status = feedback.status();
		textRenderer = MinecraftClient.getInstance().textRenderer;
		List<Text> texts = initText(feedback);
		width = Math.min(240, Math.max(TextOps.width(textRenderer, texts) + 8, 107));
		textLines = wrap(texts);
	}

	/**
	 * Show a feedback toast with the given feedback and duration.
	 *
	 * @param feedback the feedback object to show.
	 * @param duration the duration (in ms) to show the toast.
	 */
	public static void report(SwitchyFeedback feedback, int duration) {
		if (feedback.status() == SwitchyFeedbackStatus.SUCCESS && !feedback.messages().isEmpty()) {
			MinecraftClient.getInstance().getToastManager().add(new SwitchyFeedbackToast(feedback, duration));
		}
	}

	private static void outline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y + 1, x + 1, y + height - 1, color);
		context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}

	@Override
	public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
		context.fill(0, 0, getWidth(), getHeight(), 0x77000000);
		outline(context, 0, 0, getWidth(), getHeight(), colours.get(status));

		OrderedText header = textLines.get(0);
		context.drawText(textRenderer, header, ((width - textRenderer.getWidth(header)) >> 1) - 7, 4, 0xFFFFFF, true);

		for (int i = 1; i < textLines.size(); i++) {
			context.drawText(textRenderer, textLines.get(i), 4, 4 + i * 11, 0xFFFFFF, false);
		}

		return startTime > duration ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public int getHeight() {
		return 6 + textLines.size() * 11;
	}

	@Override
	public int getWidth() {
		return width;
	}

	private List<Text> initText(SwitchyFeedback feedback) {
		List<Text> texts = new ArrayList<>();
		texts.add(Feedback.literal("----Switchy----").formatted(Formatting.BOLD, Formatting.AQUA));

		texts.addAll(feedback.messages());
		return texts;
	}

	private List<OrderedText> wrap(List<Text> messages) {
		List<OrderedText> list = new ArrayList<>();
		messages.forEach(text -> list.addAll(textRenderer.wrapLines(text, getWidth() - 8)));
		return list;
	}

	@Override
	public Object getType() {
		return Type.FEEDBACK_TYPE;
	}

	private enum Type {
		FEEDBACK_TYPE
	}
}

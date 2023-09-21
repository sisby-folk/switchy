package folk.sisby.switchy.client.api.modules;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.api.modules.CardinalSerializerData;
import folk.sisby.switchy.client.api.PrettyElementVisitor;
import folk.sisby.switchy.client.api.module.SwitchyClientModule;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.ui.api.SwitchyUIPosition;
import folk.sisby.switchy.ui.api.module.SwitchyUIModule;
import folk.sisby.switchy.util.Feedback;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.BundleTooltipData;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A generic module for previewing Switchy Cardinal module data using NBT paths.
 *
 * @author Sisby folk
 * @see SwitchyUIModule
 * @see folk.sisby.switchy.api.modules.CardinalSerializerModule
 * @since 2.6.0
 */
@ClientOnly
public class CardinalSerializerClientModule extends CardinalSerializerData implements SwitchyClientModule, SwitchyUIModule {
	private final Identifier id;
	private final PreviewConfig config;

	private CardinalSerializerClientModule(Identifier id, PreviewConfig config) {
		this.id = id;
		this.config = config;
	}

	public static void register(Identifier id, PreviewConfig config) throws IllegalArgumentException, IllegalStateException {
		SwitchyClientModuleRegistry.registerModule(id, () -> new CardinalSerializerClientModule(id, config));
	}

	@Override
	public Pair<Component, SwitchyUIPosition> getPreviewComponent(String presetName) {
		if (moduleNbt.isEmpty() || config.failsCondition(moduleNbt)) return null;

		ItemStack stack = config.icon.apply(moduleNbt);
		List<MutableText> values = config.getValues(moduleNbt);
		DefaultedList<ItemStack> items = config.getStacks(moduleNbt);

		if (values.isEmpty() && items.isEmpty() && config.conditionPath == null && !config.icon.apply(null).isOf(Items.DIRT)) return null;

		ItemComponent component = Components.item(stack);
		List<TooltipComponent> tooltips = new ArrayList<>(List.of(TooltipComponent.of(Feedback.translatable("switchy.modules.%s.%s.preview.tooltip".formatted(id.getNamespace(), id.getPath()), values.toArray()).asOrderedText())));
		if (!items.isEmpty()) tooltips.add(TooltipComponent.of(new BundleTooltipData(items, 0)));

		component.tooltip(tooltips);
		return Pair.of(component, SwitchyUIPosition.GRID_RIGHT);
	}

	public record PreviewConfig(Function<NbtCompound, ItemStack> icon, List<NbtPathArgumentType.NbtPath> valuePaths, List<NbtPathArgumentType.NbtPath> inventoryPaths, @Nullable NbtPathArgumentType.NbtPath conditionPath) {
		public List<MutableText> getValues(NbtCompound nbt) {
			return valuePaths.stream().map(v -> {
				try {
					MutableText text = Feedback.literal("");
					v.get(nbt).stream().map(new PrettyElementVisitor()::apply).forEach(text::append);
					if (text.getString().isEmpty()) text = Feedback.literal("???");
					return text;
				} catch (CommandSyntaxException e) {
					return Feedback.literal("???");
				}
			}).toList();
		}

		public DefaultedList<ItemStack> getStacks(NbtCompound nbt) {
			DefaultedList<ItemStack> items = DefaultedList.of();
			inventoryPaths.forEach(v -> {
				try {
					v.get(nbt).forEach(e1 -> {
						if (e1 instanceof NbtCompound c1) {
							c1.getList("Items", 10).forEach(e2 -> {
								ItemStack stack = ItemStack.fromNbt((NbtCompound) e2);
								if (!stack.isEmpty()) items.add(stack);
							});
						}
					});
				} catch (CommandSyntaxException | ClassCastException ignored) {}
			});
			return items;
		}

		public boolean failsCondition(NbtCompound nbt) {
			if (conditionPath != null) {
				try {
					return conditionPath.get(nbt).get(0) instanceof NbtByte nb && nb.byteValue() == 0;
				} catch (CommandSyntaxException | IndexOutOfBoundsException ignored) {
				}
			}
			return false;
		}
	}
}

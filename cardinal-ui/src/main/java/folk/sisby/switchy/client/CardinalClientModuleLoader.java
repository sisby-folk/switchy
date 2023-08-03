package folk.sisby.switchy.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import folk.sisby.switchy.client.api.module.SwitchyClientModuleRegistry;
import folk.sisby.switchy.client.api.modules.CardinalSerializerClientModule;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.qsl.resource.loader.api.reloader.IdentifiableResourceReloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A resource loader responsible for creating data-driven {@link folk.sisby.switchy.ui.api.module.SwitchyUIModule}s for previewing Switchy Cardinal module data.
 * Will load json files in {@code assets/{namespace}/switchy_cardinal/{path}.json} into modules with {@code namespace:path} IDs in the below format:
 * {@code
 * {
 * "icon": {"item": "minecraft:air"}
 * "values": ["nbt.paths"]
 * }
 * }
 *
 * @author Sisby folk
 * @see CardinalSerializerClientModule
 * @since 2.6.0
 */
public class CardinalClientModuleLoader extends JsonDataLoader implements IdentifiableResourceReloader {
	/**
	 * The global instance for this resource loader.
	 */
	public static final CardinalClientModuleLoader INSTANCE = new CardinalClientModuleLoader(new Gson());
	private static final Identifier ID = new Identifier(SwitchyCardinalClient.ID, "module_loader");
	private static final String KEY_CONDITION = "condition";
	private static final String KEY_ICON = "icon";
	private static final String KEY_ICON_PATH = "path";
	private static final String KEY_INVENTORIES = "inventories";
	private static final String KEY_VALUES = "values";
	private static final NbtPathArgumentType pathAtg = NbtPathArgumentType.nbtPath();

	CardinalClientModuleLoader(Gson gson) {
		super(gson, "switchy_cardinal");
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		prepared.forEach((moduleId, contents) -> {
			if (SwitchyClientModuleRegistry.containsModule(moduleId)) {
				return;
			}
			JsonObject moduleOptions = contents.getAsJsonObject();
			if (!moduleOptions.has(KEY_ICON)) {
				SwitchyCardinalClient.LOGGER.warn("[Switchy Cardinal UI] module '{}' is missing options, skipping...", moduleId);
				return;
			}

			try {
				Function<NbtCompound, ItemStack> iconStackSupplier;
				JsonObject icon = moduleOptions.get(KEY_ICON).getAsJsonObject();
				if (icon.has(KEY_ICON_PATH)) {
					try {
						NbtPathArgumentType.NbtPath path = pathAtg.parse(new StringReader(icon.get(KEY_ICON_PATH).getAsString()));
						iconStackSupplier = nbt -> {
							try {
								return ItemStack.fromNbt((NbtCompound) path.get(nbt).get(0));
							} catch (Exception e) {
								return Items.DIRT.getDefaultStack();
							}
						};
					} catch (CommandSyntaxException e) {
						SwitchyCardinalClient.LOGGER.warn("[Switchy Cardinal UI] module '{}' has invalid condition path '{}', skipping...", moduleId, icon.get(KEY_ICON_PATH).getAsString());
						return;
					}
				} else {
					ItemStack stack = ItemStack.fromNbt((NbtCompound) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, icon));
					if (stack.getCount() == 0) stack.setCount(1);
					iconStackSupplier = nbt -> stack;
				}

				List<NbtPathArgumentType.NbtPath> valuePaths = new ArrayList<>();
				if (moduleOptions.has(KEY_VALUES)) {
					for (JsonElement element : moduleOptions.getAsJsonArray(KEY_VALUES)) {
						try {
							valuePaths.add(pathAtg.parse(new StringReader(element.getAsString())));
						} catch (CommandSyntaxException e) {
							SwitchyCardinalClient.LOGGER.warn("[Switchy Cardinal UI] module '{}' has invalid path '{}', skipping...", moduleId, element.getAsString());
							return;
						}
					}
				}

				List<NbtPathArgumentType.NbtPath> inventoryPaths = new ArrayList<>();
				if (moduleOptions.has(KEY_INVENTORIES)) {
					for (JsonElement element : moduleOptions.getAsJsonArray(KEY_INVENTORIES)) {
						try {
							inventoryPaths.add(pathAtg.parse(new StringReader(element.getAsString())));
						} catch (CommandSyntaxException e) {
							SwitchyCardinalClient.LOGGER.warn("[Switchy Cardinal UI] module '{}' has invalid path '{}', skipping...", moduleId, element.getAsString());
							return;
						}
					}
				}

				NbtPathArgumentType.NbtPath condition = null;
				if (moduleOptions.has(KEY_CONDITION)) {
					try {
						condition = pathAtg.parse(new StringReader(moduleOptions.get(KEY_CONDITION).getAsString()));
					} catch (CommandSyntaxException e) {
						SwitchyCardinalClient.LOGGER.warn("[Switchy Cardinal UI] module '{}' has invalid condition path '{}', skipping...", moduleId, moduleOptions.get(KEY_CONDITION).getAsString());
						return;
					}
				}

				CardinalSerializerClientModule.register(moduleId, new CardinalSerializerClientModule.PreviewConfig(iconStackSupplier, valuePaths, inventoryPaths, condition));
			} catch (UnsupportedOperationException | JsonSyntaxException ignoredGetFromJsonEx) {
				SwitchyCardinalClient.LOGGER.warn("[Switchy Cardinal UI] module '{}' has invalid types, skipping...", moduleId);
			}
		});
		SwitchyCardinalClient.LOGGER.info("[Switchy Cardinal UI] Finished reloading {} modules!", prepared.size());
	}

	@Override
	public @NotNull Identifier getQuiltId() {
		return ID;
	}
}

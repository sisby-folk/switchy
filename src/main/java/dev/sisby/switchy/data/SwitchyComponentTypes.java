package dev.sisby.switchy.data;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.FormatUtils;
import dev.sisby.switchy.util.SwitchyCodecs;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

	public static final SwitchyComponentType<Text> NAME = register(Switchy.id("name"), TextCodecs.CODEC, b -> b.textProvider(c -> c)
		.argumentEditor(e -> CommandManager.argument("name", StringArgumentType.greedyString()).executes(c -> e.execute(c, Text.of(c.getArgument("name", String.class))))));
	public static final SwitchyComponentType<Float> HEALTH = register(Switchy.id("health"), Codec.FLOAT, b -> b.nbtSwitcher("Health").textProvider(FormatUtils::healthText));
	public static final SwitchyComponentType<Vec3d> POS = register(Switchy.id("pos"), Vec3d.CODEC, b -> b.nbtSwitcher("Pos").textProvider(c -> Text.of(BlockPos.ofFloored(c).toShortString())));
	public static final SwitchyComponentType<Identifier> DIMENSION = register(Switchy.id("dimension"), Identifier.CODEC, b -> b.nbtSwitcher("Dimension").textProvider(c -> Text.of(FormatUtils.prettify(c.getPath()))));
	public static final SwitchyComponentType<DefaultedList<ItemStack>> INVENTORY = register(Switchy.id("inventory"), SwitchyCodecs.INVENTORY_CODEC, b -> b.nbtSwitcher("Inventory").textProvider(FormatUtils::inventoryText).emptyChecker(dl -> dl.stream().allMatch(ItemStack::isEmpty)));

	public static void init() {
		// static init
	}

	public static <T> SwitchyComponentType<T> register(Identifier id, Codec<T> codec, UnaryOperator<SwitchyComponentType.Builder<T>> operations) {
		return instance().register(id, i -> operations.apply(SwitchyComponentType.builder(i, codec)).build());
	}

	public static SwitchyComponentTypes instance() {
		return INSTANCE;
	}
}

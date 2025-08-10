package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.function.UnaryOperator;

public class SwitchyComponentTypes extends TypeRegistry<SwitchyComponentType<?>> {
	private static final SwitchyComponentTypes INSTANCE = new SwitchyComponentTypes();

	public static final SwitchyComponentType<Text> NAME = register(Switchy.id("name"), TextCodecs.CODEC, b -> b);
	public static final SwitchyComponentType<Float> HEALTH = register(Switchy.id("health"), Codec.FLOAT, b -> b.nbtSwitcher("Health"));
	public static final SwitchyComponentType<Vec3d> POS = register(Switchy.id("pos"), Vec3d.CODEC, b -> b.nbtSwitcher("Pos"));
	public static final SwitchyComponentType<Identifier> DIMENSION = register(Switchy.id("dimension"), Identifier.CODEC, b -> b.nbtSwitcher("Dimension"));

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

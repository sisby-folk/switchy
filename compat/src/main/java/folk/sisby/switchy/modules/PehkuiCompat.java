package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A module that switches scale values from Virtuoel's Pehkui.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.3.0
 */
public class PehkuiCompat implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "pehkui");

	/**
	 * The NBT keys where each scale value is stored, per ScaleType.
	 */
	public static final Map<ScaleType, String> scaleKeys = new HashMap<>();

	static {
		SwitchyModuleRegistry.registerModule(ID, PehkuiCompat::new, new SwitchyModuleInfo(true, SwitchyModuleEditable.OPERATOR));
		List.of(ScaleTypes.HEIGHT, ScaleTypes.WIDTH, ScaleTypes.MODEL_HEIGHT, ScaleTypes.MODEL_WIDTH).forEach(PehkuiCompat::addScaleType);
	}

	/**
	 * The value of each ScaleType.
	 */
	public final Map<ScaleType, @Nullable Float> scaleValues = new HashMap<>();

	PehkuiCompat() {
		scaleKeys.forEach((type, key) -> scaleValues.put(type, null));
	}

	/**
	 * @param type the scale type to request be switched.
	 */
	public static void addScaleType(ScaleType type) {
		scaleKeys.put(type, ScaleRegistries.SCALE_TYPES.inverse().get(type).getPath());
	}

	/**
	 * Executes {@code static} the first time it's invoked.
	 */
	public static void touch() {
	}

	@Override
	public void updateFromPlayer(ServerPlayerEntity player, @Nullable String nextPreset) {
		scaleValues.replaceAll((t, v) -> t.getScaleData(player).getTargetScale());
	}

	@Override
	public void applyToPlayer(ServerPlayerEntity player) {
		scaleValues.forEach((type, value) -> {
			if (value != null) type.getScaleData(player).setTargetScale(value);
		});
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		scaleValues.forEach((type, value) -> {
			if (value != null) outNbt.putFloat(scaleKeys.get(type), value);
		});
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		scaleKeys.forEach((type, key) -> {
			if (nbt.contains(key)) scaleValues.put(type, nbt.getFloat(key));
		});
	}
}

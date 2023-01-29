package folk.sisby.switchy.modules;

import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.PresetModule;
import folk.sisby.switchy.api.PresetModuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PehkuiCompat implements PresetModule {
	public static final Identifier ID = new Identifier("switchy", "pehkui");

	public static final Map<ScaleType, String> scaleKeys = new HashMap<>();
	public final Map<ScaleType, @Nullable Float> scaleValues = new HashMap<>();

	@Override
	public void updateFromPlayer(PlayerEntity player, @Nullable String nextPreset) {
		scaleValues.replaceAll((t, v) -> t.getScaleData(player).getTargetScale());
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		scaleValues.forEach((type, value) -> {if (value != null) type.getScaleData(player).setTargetScale(value);});
	}

	@Override
	public NbtCompound toNbt() {
		NbtCompound outNbt = new NbtCompound();
		scaleValues.forEach((type, value) -> {if (value != null) outNbt.putFloat(scaleKeys.get(type), value);});
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		scaleKeys.forEach((type, key) -> {if (nbt.contains(key)) scaleValues.put(type, nbt.getFloat(key));});
	}

	public PehkuiCompat() {
		scaleKeys.forEach((type, key) -> scaleValues.put(type, null));
	}

	public static void touch() {
	}

	public static void addScaleType(ScaleType type) {
		scaleKeys.put(type, ScaleRegistries.SCALE_TYPES.inverse().get(type).getPath());
	}

	// Runs on touch() - but only once.
	static {
		PresetModuleRegistry.registerModule(ID, PehkuiCompat::new, true, ModuleImportable.OPERATOR);
		List.of(ScaleTypes.HEIGHT, ScaleTypes.WIDTH, ScaleTypes.MODEL_HEIGHT, ScaleTypes.MODEL_WIDTH).forEach(PehkuiCompat::addScaleType);
	}
}

package folk.sisby.switchy.modules;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.api.module.SwitchyModule;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.module.SwitchyModuleInfo;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.config.PehkuiModuleConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;

import java.util.HashMap;
import java.util.Map;

import static folk.sisby.switchy.util.Feedback.translatable;

/**
 * A module that switches scale values from Virtuoel's Pehkui.
 *
 * @author Sisby folk
 * @see SwitchyModule
 * @since 1.3.0
 */
public class PehkuiModule implements SwitchyModule {
	/**
	 * Identifier for this module.
	 */
	public static final Identifier ID = new Identifier("switchy", "pehkui");
	/**
	 * The config object for the pehkui module, containing the current state of {@code /config/switchy/pehkui.toml}.
	 */
	@SuppressWarnings("deprecation")
	public static final PehkuiModuleConfig CONFIG = PehkuiModuleConfig.create(FabricLoader.getInstance().getConfigDir(), Switchy.ID, "pehkui", PehkuiModuleConfig.class);

	/**
	 * Registers the module
	 */
	public static void register() {
		SwitchyModuleRegistry.registerModule(ID, PehkuiModule::new, new SwitchyModuleInfo(
				true,
				SwitchyModuleEditable.OPERATOR,
				translatable("switchy.modules.switchy.pehkui.description")
			)
				.withDescriptionWhenEnabled(translatable("switchy.modules.switchy.pehkui.enabled"))
				.withDescriptionWhenDisabled(translatable("switchy.modules.switchy.pehkui.disabled"))
				.withDeletionWarning(translatable("switchy.modules.switchy.pehkui.warning"))
		);
	}

	/**
	 * The value of each ScaleType.
	 */
	public final Map<ScaleType, @Nullable Float> scaleValues = new HashMap<>();

	PehkuiModule() {
		CONFIG.scaleTypes.forEach(id -> scaleValues.put(ScaleRegistries.SCALE_TYPES.get(new Identifier(id)), null));
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
			if (value != null)
				outNbt.putFloat(ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, type).toString(), value);
		});
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		scaleValues.forEach((type, value) -> {
			String path = ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, type).toString();
			if (nbt.contains(path)) scaleValues.put(type, nbt.getFloat(path));
		});
	}
}

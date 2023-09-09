package folk.sisby.switchy.ui.mixin.compat;

import folk.sisby.switchy.client.SwitchyClient;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * A simple mixin plugin that loads mixins in sub-folders if they match the ID of a loaded mod.
 *
 * @author SilverAndro
 * @since 1.7.2
 */
@SuppressWarnings("deprecation")
public class MixinCompatPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith("folk.sisby.switchy.ui.mixin.compat.")) {
			int startModID = mixinClassName.indexOf(".compat.") + ".compat.".length();
			int endModID = mixinClassName.indexOf('.', startModID);
			String modID = mixinClassName.substring(startModID, endModID);
			return FabricLoader.getInstance().isModLoaded(modID) || FabricLoader.getInstance().isModLoaded(modID.replaceAll("_", "-"));
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}
}

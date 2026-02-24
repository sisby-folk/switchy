package dev.sisby.switchy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import dev.sisby.switchy.data.SwitchyComponentType;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.duck.SwitchyGameProfile;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GameProfile.class, remap = false)
public class MixinGameProfile implements SwitchyGameProfile {
	private SwitchyProfile switchy$sayProfile = null;

	@Override
	public SwitchyProfile switchy$getSayProfile() {
		return switchy$sayProfile;
	}

	@Override
	public void switchy$setSayProfile(SwitchyProfile profile) {
		switchy$sayProfile = profile;
	}

	@ModifyReturnValue(method = "getProperties", at = @At("RETURN"))
	private PropertyMap overrideSkinDuringSayForBridges(PropertyMap original) {
		if (switchy$sayProfile != null && SwitchyComponentTypes.instance() != null) {
			SwitchyComponentType<NbtCompound> skin = (SwitchyComponentType<NbtCompound>) SwitchyComponentTypes.instance().get(SwitchyComponentTypes.TAILOR_SKIN);
			if (skin != null && switchy$sayProfile.contains(skin)) {
				NbtCompound compound = switchy$sayProfile.get(skin);
				PropertyMap newMap = new PropertyMap();
				newMap.putAll(original);
				newMap.removeAll("textures");
				newMap.put("textures", new Property("textures", compound.getString("value"), compound.getString("signature")));
				return newMap;
			}
		}
		return original;
	}
}

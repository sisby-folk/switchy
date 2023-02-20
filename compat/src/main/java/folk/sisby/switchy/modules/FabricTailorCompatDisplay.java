package folk.sisby.switchy.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.datafixers.util.Pair;
import com.mojang.util.UUIDTypeAdapter;
import folk.sisby.switchy.SwitchyDisplayModules;
import folk.sisby.switchy.api.module.SwitchyDisplayModule;
import folk.sisby.switchy.client.screen.SwitchScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Base64;
import java.util.UUID;

public class FabricTailorCompatDisplay extends FabricTailorCompatData implements SwitchyDisplayModule<FabricTailorCompat> {
	@Override
	public void fillFromData(FabricTailorCompat fabricTailorCompat) {
		fillFromNbt(fabricTailorCompat.toNbt());
	}

	@Override
	public Pair<Component, SwitchScreen.ComponentPosition> getDisplayComponent() {
		if (skinValue == null) return null;
		MinecraftClient client = MinecraftClient.getInstance();

		Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
		MinecraftTexturesPayload payload = gson.fromJson(new String(Base64.getDecoder().decode(skinValue)), MinecraftTexturesPayload.class);
		MinecraftProfileTexture skinTexture = payload.getTextures().get(MinecraftProfileTexture.Type.SKIN);

		Identifier skinId = client.getSkinProvider().loadSkin(skinTexture, MinecraftProfileTexture.Type.SKIN);

		EntityComponent<AbstractClientPlayerEntity> skinPreview = Components.entity(Sizing.fixed(60), new AbstractClientPlayerEntity(client.world, client.getSession().getProfile(), null) {
			@Override
			public Identifier getSkinTexture() {
				return skinId;
			}
		});

		skinPreview.scale(0.5F);

		return Pair.of(skinPreview, SwitchScreen.ComponentPosition.SIDE_RIGHT);
	}

	public static void touch() {}

	static {
		SwitchyDisplayModules.registerModule(ID, FabricTailorCompatDisplay::new);
	}
}

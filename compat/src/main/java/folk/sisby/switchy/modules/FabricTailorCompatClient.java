package folk.sisby.switchy.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import folk.sisby.switchy.client.api.SwitchyScreenExtensions;
import folk.sisby.switchy.client.screen.SwitchScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Base64;
import java.util.UUID;

public class FabricTailorCompatClient {
	public static final Identifier ID = new Identifier("switchy", "fabric_tailor");

	public static final String KEY_SKIN_VALUE = "skinValue";
	public static final String KEY_SKIN_SIGNATURE = "skinSignature";

	public static void touch() {}

	static {
		SwitchyScreenExtensions.registerQuickSwitchDisplayComponent(ID, SwitchScreen.ComponentPosition.SIDE_RIGHT, displayPreset -> {
			if (!displayPreset.modules.containsKey(ID)) return null;
			NbtCompound nbt = displayPreset.modules.get(ID);
			if (!nbt.contains(KEY_SKIN_VALUE) || !nbt.contains(KEY_SKIN_SIGNATURE)) return null;
			MinecraftClient client = MinecraftClient.getInstance();

			String value = nbt.getString(KEY_SKIN_VALUE);
			Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
			MinecraftTexturesPayload payload = gson.fromJson(new String(Base64.getDecoder().decode(value)), MinecraftTexturesPayload.class);
			MinecraftProfileTexture skinTexture = payload.getTextures().get(MinecraftProfileTexture.Type.SKIN);

			Identifier skinId = client.getSkinProvider().loadSkin(skinTexture, MinecraftProfileTexture.Type.SKIN);

			EntityComponent<AbstractClientPlayerEntity> skinPreview = Components.entity(Sizing.fixed(60), new AbstractClientPlayerEntity(client.world, client.getSession().getProfile(), null) {
				@Override
				public Identifier getSkinTexture() {
					return skinId;
				}
			});

			skinPreview.scale(0.5F);

			return skinPreview;
		});
	}
}

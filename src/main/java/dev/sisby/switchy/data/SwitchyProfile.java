package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record SwitchyProfile(String id, SwitchyComponentMap components) implements SwitchyComponentHolder<SwitchyProfile> {
	public static Codec<SwitchyProfile> codec(SwitchyComponentTypes types, String id) {
		return SwitchyComponentMap.codec(types).xmap(m -> new SwitchyProfile(id, m), SwitchyProfile::components);
	}

	@Override
	public String toString() {
		return id + "\n" + components.toString();
	}

	public Collection<Component> asTexts(ServerPlayer player) {
		List<Component> outList = new ArrayList<>(List.of(Component.empty().append(Component.literal("id: ").withStyle(ChatFormatting.GRAY)).append(id)));
		outList.addAll(components().asTexts(player.createCommandSourceStack().getServer()));
		return outList;
	}

	public SwitchyProfile withId(String newId) {
		return new SwitchyProfile(newId, components);
	}
}

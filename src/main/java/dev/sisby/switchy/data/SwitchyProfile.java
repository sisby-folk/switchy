package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

	public Collection<Text> asTexts(ServerPlayerEntity player) {
		List<Text> outList = new ArrayList<>(List.of(Text.empty().append(Text.literal("id: ").formatted(Formatting.GRAY)).append(id)));
		outList.addAll(components().asTexts(player.getServer()));
		return outList;
	}

	public SwitchyProfile withId(String newId) {
		return new SwitchyProfile(newId, components);
	}
}

package dev.sisby.switchy.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record SwitchyProfile(String id, SwitchyComponentMap components) implements SwitchyComponentHolder<SwitchyProfile> {
	public static Codec<SwitchyProfile> codec(String id) {
		return SwitchyComponentMap.CODEC.xmap(m -> new SwitchyProfile(id, m), SwitchyProfile::components);
	}

	public static PacketCodec<RegistryByteBuf, SwitchyProfile> packetCodec(String id) {
		return SwitchyComponentMap.PACKET_CODEC.xmap(m -> new SwitchyProfile(id, m), SwitchyProfile::components);
	}

	@Override
	public String toString() {
		return id + "\n" + components.toString();
	}
}

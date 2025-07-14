package dev.sisby.switchy.data;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.util.TypeRegistry;

public class SwitchyLayerTypes extends TypeRegistry<SwitchyLayerType> {
	private static final SwitchyLayerTypes INSTANCE = new SwitchyLayerTypes();
	public static SwitchyLayerTypes instance() {
		return INSTANCE;
	}

	public static final SwitchyLayerType PRIMARY = instance().register(Switchy.id("primary"), id -> SwitchyLayerType.builder(id).build());
	public static final SwitchyLayerType SOCIAL = instance().register(Switchy.id("social"), id -> SwitchyLayerType.builder(id).build());

	public static void init() {
	}
}

package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.lua.FiguraAPI;
import org.moon.figura.lua.LuaWhitelist;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FiguraCompat implements FiguraAPI {
	@Override
	public FiguraAPI build(Avatar avatar) {
		return new FiguraCompat();
	}

	@Override
	public String getName() {
		return "switchy";
	}

	@Override
	public Collection<Class<?>> getWhitelistedClasses() {
		return List.of(this.getClass());
	}

	@LuaWhitelist
	public static void register(LuaFunction function) {
		SwitchyClientEvents.SWITCH.register((event) -> function.invoke(
				LuaValue.varargsOf(new LuaValue[]{
						LuaValue.valueOf(event.player.toString()),
						LuaValue.valueOf(Objects.requireNonNullElse(event.currentPreset, "")),
						LuaValue.valueOf(Objects.requireNonNullElse(event.previousPreset, "")),
						LuaValue.listOf(event.enabledModules.stream().map(LuaValue::valueOf).toArray(LuaValue[]::new))
				})
		));
	}
}

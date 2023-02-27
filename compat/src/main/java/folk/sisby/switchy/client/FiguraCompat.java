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

/**
 * @author Sisby folk
 * @see SwitchyClientEvents
 * A Figura API that exposes Switchy events to its avatar scripting system
 * @since 2.0.0
 */
public class FiguraCompat implements FiguraAPI {
	/**
	 * @param function a lua callback (UUID, current, previous, modules[])
	 * @see SwitchyClientEvents#SWITCH
	 */
	@LuaWhitelist
	public static void registerSwitchListener(LuaFunction function) {
		SwitchyClientEvents.SWITCH.register((event) -> function.invoke(
				LuaValue.varargsOf(new LuaValue[]{
						LuaValue.valueOf(event.player().toString()),
						LuaValue.valueOf(Objects.requireNonNullElse(event.currentPreset(), "")),
						LuaValue.valueOf(Objects.requireNonNullElse(event.previousPreset(), "")),
						LuaValue.listOf(event.enabledModules().stream().map(LuaValue::valueOf).toArray(LuaValue[]::new))
				})
		));
	}

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
		return List.of(getClass());
	}
}

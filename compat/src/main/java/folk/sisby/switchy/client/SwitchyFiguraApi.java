package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.entries.FiguraAPI;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;

import java.util.Collection;
import java.util.List;

/**
 * A Figura API that exposes Switchy events to its avatar scripting system.
 *
 * @author Sisby folk
 * @see SwitchyClientEvents
 * @since 2.0.0
 */
@LuaWhitelist
public class SwitchyFiguraApi implements FiguraAPI {
	/**
	 * Registers a lua listener for switch events.
	 *
	 * @param function a lua callback (UUID, current, previous, modules[]).
	 * @see SwitchyClientEvents#SWITCH
	 */
	@LuaWhitelist
	public static void registerSwitchListener(@LuaNotNil LuaFunction function) {
		SwitchyClientEvents.SWITCH.register((event) -> function.invoke(
				LuaValue.varargsOf(new LuaValue[]{
						LuaValue.valueOf(event.player().toString()),
						event.currentPreset() != null ? LuaValue.valueOf(event.currentPreset()) : LuaValue.NIL,
						event.previousPreset() != null ? LuaValue.valueOf(event.previousPreset()) : LuaValue.NIL,
						LuaValue.listOf(event.enabledModules().stream().map(LuaValue::valueOf).toArray(LuaValue[]::new))
				})
		));
	}

	@Override
	public FiguraAPI build(Avatar avatar) {
		return new SwitchyFiguraApi();
	}

	@Override
	public String getName() {
		return "switchy";
	}

	@Override
	public Collection<Class<?>> getWhitelistedClasses() {
		return List.of(this.getClass());
	}

	@Override
	public Collection<Class<?>> getDocsClasses() {
		return List.of();
	}

	@Override
	public String toString() {
		return "SwitchyFiguraApi";
	}
}

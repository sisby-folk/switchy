package folk.sisby.switchy.client;

import folk.sisby.switchy.client.api.SwitchyClientEvents;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.entries.FiguraAPI;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Figura API that exposes Switchy events to its avatar scripting system.
 *
 * @author Sisby folk
 * @see SwitchyClientEvents
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@LuaWhitelist
public class SwitchyFiguraApi implements FiguraAPI {
	public static Map<Avatar, LuaFunction> AVATAR_LISTENERS = new HashMap<>();
	public @NotNull Avatar forAvatar;

	static {
		SwitchyClientEvents.SWITCH.register((event) -> AVATAR_LISTENERS.forEach((avatar, function) -> {
			if (avatar.scriptError) return;
			try {
				function.invoke(
					LuaValue.varargsOf(new LuaValue[]{
						LuaValue.valueOf(event.player().toString()),
						event.currentPreset() != null ? LuaValue.valueOf(event.currentPreset()) : LuaValue.NIL,
						event.previousPreset() != null ? LuaValue.valueOf(event.previousPreset()) : LuaValue.NIL,
						LuaValue.listOf(event.enabledModules().stream().map(LuaValue::valueOf).toArray(LuaValue[]::new))
					})
				);
			} catch (LuaError e) {
				avatar.luaRuntime.error(e);
			}
		}));
	}

	public SwitchyFiguraApi() {}

	private SwitchyFiguraApi(@NotNull Avatar avatar) {
		forAvatar = avatar;
	}

	/**
	 * Registers a lua listener for switch events.
	 *
	 * @param function a lua callback (UUID, current, previous, modules[]).
	 * @see SwitchyClientEvents#SWITCH
	 */
	@LuaWhitelist
	public void registerSwitchListener(@LuaNotNil LuaFunction function) {
		AVATAR_LISTENERS.put(forAvatar, function);
	}

	@Override
	public FiguraAPI build(Avatar avatar) {
		return new SwitchyFiguraApi(avatar);
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

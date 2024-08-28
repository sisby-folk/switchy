package folk.sisby.switchy.client;

import com.mojang.datafixers.util.Pair;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.entries.FiguraEvent;
import org.figuramc.figura.entries.annotations.FiguraEventPlugin;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.figuramc.figura.lua.docs.LuaFieldDoc;

import java.util.Collection;
import java.util.List;

@FiguraEventPlugin
public class SwitchyFiguraEvents implements FiguraEvent {
	@LuaWhitelist
	@LuaFieldDoc("events.switchy.world_switch")
	public static LuaEvent WORLD_SWITCH = new LuaEvent();
	@LuaWhitelist
	@LuaFieldDoc("events.switchy.switch")
	public static LuaEvent SWITCH = new LuaEvent();

	static {
		SwitchyClientEvents.SWITCH.register((event) -> {
			Avatar avatar = AvatarManager.getAvatarForPlayer(event.player());

			if (avatar != null) {
				String newPreset = event.currentPreset();
				String oldPreset = event.previousPreset();
				List<String> enabledModules = event.enabledModules();

				avatar.run(WORLD_SWITCH, avatar.worldTick, newPreset, oldPreset, enabledModules);

				if (avatar.loaded && avatar.luaRuntime != null && avatar.luaRuntime.entityAPI != null && avatar.luaRuntime.entityAPI.isLoaded())
					avatar.run(SWITCH, avatar.tick, newPreset, oldPreset, enabledModules);
			}
		});
	}

	@Override
	public String getID() {
		return "switchy";
	}

	@Override
	public Collection<Pair<String, LuaEvent>> getEvents() {
		return List.of(
			new Pair<>("SWITCH", SWITCH),
			new Pair<>("WORLD_SWITCH", WORLD_SWITCH)
		);
	}
}

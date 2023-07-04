<center><img alt="switchy banner" src="https://user-images.githubusercontent.com/55819817/198210616-eb37be12-cd96-40c8-a941-68a96b2aadfc.png" /></center>

<center>An extensible preset system for player customizations provided by other mods.<br/>
Works in singleplayer and on server-side.<br/>
Requires <a href="https://modrinth.com/mod/owo-lib">oωo</a> on client-side.
</center>

---

<center><b>Packs:</b> <a href="https://modrinth.com/modpack/tinkerers-quilt">Tinkerer's Quilt</a> (<a href="https://modrinth.com/modpack/tinkerers-silk">Silk</a>) - <a href="https://modrinth.com/modpack/switchy-pack">Switchy Pack</a></center>
<center><b>Mods:</b> <i>Switchy</i> - <a href="https://modrinth.com/mod/origins-minus">Origins Minus</a> (<a href="https://modrinth.com/mod/tinkerers-statures">Statures</a>) - <a href="https://modrinth.com/mod/tinkerers-smithing">Tinkerer's Smithing</a></center>

---

## What is Switchy?

Switchy lets you use commands make ***presets*** that are stored with your player data.

Switchy will load ***modules*** that tell presets what to store.

When you ***switch*** presets, that data is saved to the old preset, then loaded from the new one.

## Modules

Most modules provide inter-compatibility with other mods - be sure to follow the links.

You can hotswap these features out-of-the box by installing their relevant mods:

- Player Nicknames/Pronouns with either of:
    - [Drogtor The Nickinator](https://modrinth.com/mod/drogtor)
    - [Styled Nicknames](https://modrinth.com/mod/styled-nicknames) (Note: Switchy force-allows nickname
      self-assignment)
- Player Skin with [Fabric Tailor](https://modrinth.com/mod/fabrictailor) or model with [Figura](https://modrinth.com/mod/figura) (via API)
- Player Origin with [Origins](https://modrinth.com/mod/origins) (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures))
    - Power state / command powers - e.g. Origin inventories and `/power grant` powers (via [MerchantPug](https://github.com/MerchantPug))
- Player sizes and scales with [Pehkui](https://modrinth.com/mod/pehkui)
- Detailed player profiles for conventions with [Lanyard](https://modrinth.com/mod/lanyard)

More functionality can be added with these Addons:

- [Switchy Inventories](https://modrinth.com/mod/switchy-inventories) - separate inventories, ender chests, and
  trinkets (all disabled by default)
- [Switchy Teleport](https://modrinth.com/mod/switchy-teleport) - separate player position and spawn points (all
  disabled by default)
- [SwitchyKit](https://modrinth.com/mod/switchykit) - import presets with nicknames (as above) with colours, pronouns
  and system tags - directly from Pluralkit or Tupperbox.
- [Switchy Resource Packs](https://modrinth.com/mod/switchy-resource-packs) - separate enabled resource packs per
  preset.
- [Switchy Proxy](https://modrinth.com/mod/switchy-proxy) - single-message nickname switching with custom patterns using
  either nickname mod.

These mods have Switchy support built-in (or built in to switchy):

- [RPGStats](https://modrinth.com/mod/rpgstats) - All stat levels and XP can be kept per-preset
- [Hexcasting](https://modrinth.com/mod/hex-casting) - Internalized pigment can be kept per-preset (via [leo60228](https://github.com/leo60228))

## Showcase

<iframe width="896" height="504" src="https://www.youtube.com/embed/Ht9kx40UV5I" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

Older showcase that shows fully setting up presets using commands:

<iframe width="896" height="504" src="https://www.youtube.com/embed/gkOGZUJOtR4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## Quick Start Guide

Remember, switching does nothing on its own! Make sure you install a mod from above.

### Via Server Commands

1. Use `/switchy list` to see your current presets

2. use `/switchy rename default [name]` to give your starting preset a name

3. `/switchy new [name]` will create and switch to a new preset

4. `/switchy set [name]` or `/switch [name]` will switch between existing presets

#### Toggling Modules

When a module is **Enabled**, it makes things "switch" (load and save) per-preset.

`/switchy module enable/disable [name]` will toggle this for your presets.

`/switchy module help` will tell you about a module, and what enabling it does.

### Via Client UI

With Switchy also installed on the client, click `;` to open the UI.

The Quick-Switcher allows you to preview and switch presets by clicking on them.<br/>
![image](https://user-images.githubusercontent.com/55819817/224468718-55137a82-8269-4ce6-9bb5-0c5ce8322a68.png)

Clicking the manage button shows the *manage presets* screen, allowing creating, renaming, and deleting presets.<br/>
![image](https://user-images.githubusercontent.com/55819817/224468651-95630575-72dd-4a8b-b59d-371d1f5ae86d.png)

Clicking the modules button shows the *manage modules* screen, allowing enabling and disabling modules.<br/>
![image](https://user-images.githubusercontent.com/55819817/224468657-98da4200-3a7c-43fc-8ade-b39e4a96abc8.png)
*Hovering over toggle buttons will show detailed information on the effects of pressing them.*<br/>

Clicking the data button shows the *manage data* screen, where you can import and export your presets to a file for use on other servers/worlds.
![image](https://user-images.githubusercontent.com/55819817/224468676-8d539912-6fee-4792-b578-544dc10849af.png)
To save to a file, choose export, choose which modules to include in the file, and click export!<br/>
To load from a file, choose import, choose which modules to import from the file, and click import!<br/>
Files are saved to `.minecraft/config/switchy/`, and are safe to copy between instances.<br/>

#### Client Data Commands

Data operations can also be performed using client-side commands:

`/switchy_client export` will export all of your presets and modules to a file.

`/switchy_client import [filename] [exclude] ` will import all *allowed* modules, except those in `[exclude]`

`/switchy_client import [filename] [exclude] [operator]` will import all *allowed* modules, except those in `[exclude]`,
adding those in `[operator]` if you're a server operator.

You can use `~` to specify no modules.

### How do I change a preset nickname? or skin?

Switch to the preset you'd like to change things for, then just do it as normal! <br/>
`/nick` for drogtor nicknames, `k` for fabric tailor skins, etc.<br/>
When you switch away, they'll be saved - and when you switch back, they'll be restored.

## Configuration

### Module Editing Permission

Switchy doesn't and will not support permissions on its basic commands, and has no way to enable or disable modules
server-wide.

However, you can minorly configure which players can import module data in `/config/switchy/config.toml`.

Modules will be listed with one of four import settings:

- `ALLOWED`: Importable by any player - can be changed to `OPERATOR` (e.g. origins)
- `OPERATOR`: Importable by operators when specified - can be changed to `ALLOWED` (e.g. inventories)
- `ALWAYS_ALLOWED`: Importable by any player - can't be changed (e.g. nicknames/skins)
- `NEVER`: Can't be imported due to technical limitations - can't be changed

### Pehkui Scale Types

The pehkui module will switch scale types by the IDs defined in `/config/switchy/pehkui.toml`.

By default, it switches height, width, model  height, and model width.

This is set by the server - so you can add anything you want to give players switch access to, and remove anything that should be only temporary and switchy might accidentally let players keep.

As an example, [Origins Minus](https://modrinth.com/mod/origins-minus) uses `pehkui:base` to give temporary buffs to players with the Sanguine origin - so this shouldn't be added to the config when it's installed!

### Apoli `/Power` Types

The apoli module will by default only restore data for powers that the preset already has (e.g. from switching in an origin).

This can be expanded to include powers granted using `/power grant` (i.e powers with the `apoli:command` source) in `/config/switchy/apoli.toml` - with whitelist and blacklist modes.

This allows, for example, adding an extra power to one preset to give it a 'hybrid origin'.

## Developers

Switchy can be added to your project using `modCompileOnly "folk.sisby:switchy-core:x.x.x"` and these repos:

```
maven { // Switchy
    url = 'https://maven.proxyfox.dev/'
    content {
        includeGroup 'folk.sisby'
    }
}
maven { // Server Translations API
    url "https://maven.nucleoid.xyz/"
}
maven { // owo-lib
    url 'https://maven.wispforest.io'
}
```

If you want to test with switchy locally, add `modLocalRuntime`.<br/>
if you're making an addon, just use `modImplementation`.<br/>

### Subprojects

`switchy-core` includes commands and the API. <br/>
`switchy-client` enables import/export commands and a client API. <br/>
`switchy-ui` provides the client-side switch and manage screens, and module-displaying API. <br/>
`switchy-cardinal` provides data-driven CCA switchy modules and an API. <br/>
`switchy-compat` provides the built-in modules for drogtor etc. <br/>
`switchy-compat-ui` adds ui support to compat. <br/>

### API

Switchy includes a rich API for both client and server addons for performing all of its basic functions.

Try `SwitchyPresets` (via `SwitchyPlayer.getPresets()`) covers most mod functions, then `SwitchyApi` provides them with text feedback, `SwitchyClientApi` provides them on the client, and `SwitchyEvents` and `SwitchyClientEvents` offer hooks for addons, registering modules, and adding new commands.

#### Figura Lua API

Listen to switches by passing a function to `switchy:registerSwitchListener`, e.g.

```
switchy:registerSwitchListener(function(playerId, oldPreset, newPreset, enabledModules)
  log("New Switch: " .. oldPreset .. " > " .. newPreset)
end)
```

The function body can then match specific preset names to swap in specific models.


### Modules

Adding new Modules allows more data to be switched per-preset. They only need to:

- Load and Save their data using NBT.
- Save their data from the player
- Load their data to the player

Just implement `SwitchyModule` and register it with `SwitchyModuleRegistry` using `SwitchyEvents.Init` -
See [Switchy Inventories](https://github.com/sisby-folk/switchy-inventories) for an example. (Remember to add the `events` entrypoint in your QMJ)

#### Client Integration

Modules can integrate with the client if they implement `SwitchyModuleTransferable` and have a matching `SwitchyClientModule` registered with `SwitchyClientModuleRegistry`.

The `SwitchySerializeable` portion of the server module can be split out and reused for both sides if the held data is usable on the client as-is.

Client modules don't do anything on their own, so implement `SwitchyUIModule` if you'd like to add previewing on the switch screen.

See inventories or [compat-ui](https://github.com/sisby-folk/switchy/tree/1.19/compat-ui/src/main/java/folk/sisby/switchy/client/modules) for varying examples.

#### Data-Driven CCA Modules

If your mod uses the [Cardinal Components API](https://github.com/OnyxStudios/Cardinal-Components-API) to store its
player/entity data, you can instead register a module using `CardinalSerializerModule.from()`, or `register()` if your component doesn't need extra sync logic besides `writeToNbt/readFromNbt` (or just use data).

Any data matching `data/*/switchy_cardinal/*.json` will be
loaded [like so](https://github.com/sisby-folk/switchy/blob/1.19/compat/src/main/resources/data/switchy/switchy_cardinal/lanyard.json):

- File namespace and name - module namespace and path.
    - `default`: boolean, module is enabled for players by default.
    - `editable`: See import configuration above.
    - `ifModsLoaded`: mod IDs to check before trying to register the module.
    - `components`: the cardinal components to swap.

## Further Info

This mod is primarily motivated by improving accessibility for [plural systems](https://morethanone.info).<br/>
❓ Check out [Plural Respect](https://pluralrespect.neocities.org).

### Afterword

All mods are built on the work of many others.

![Created for ModFest: Singularity](https://user-images.githubusercontent.com/55819817/224468033-99e9fc9c-c2e2-4ed3-8a9e-ba1d9d52f3cd.svg)

We made this mod (up to v1.2.1) for Modfest: Singularity! However, we intend to maintain this mod into the future.

This mod is included in [Tinkerer's Quilt](https://modrinth.com/modpack/tinkerers-quilt) - our modpack about ease of
play and self-expression.

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.


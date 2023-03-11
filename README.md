<center><img alt="switchy banner" src="https://user-images.githubusercontent.com/55819817/198210616-eb37be12-cd96-40c8-a941-68a96b2aadfc.png" /></center>

<center>An extensible preset system for player customizations provided by other mods.<br/>
Works in singleplayer and on server-side.</center>

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
- Player Skin with [Fabric Tailor](https://modrinth.com/mod/fabrictailor)
- Player Origin with [Origins](https://modrinth.com/mod/origins) (includes all layers,
  e.g. [Statures](https://modrinth.com/mod/tinkerers-statures))
    - [Contributed by [MerchantPug](https://github.com/MerchantPug)] Apoli power state - e.g. Origin power inventories,
      cooldowns
- Player height and size with [Pehkui](https://modrinth.com/mod/pehkui)
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

These mods have Switchy support built-in:

- [RPGStats](https://modrinth.com/mod/rpgstats) - All 6 stats can be kept per-preset

## Showcase

<iframe width="896" height="504" src="https://www.youtube.com/embed/Ht9kx40UV5I" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

Older showcase that shows fully setting up presets using commands:

<iframe width="896" height="504" src="https://www.youtube.com/embed/gkOGZUJOtR4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## Quick Start Guide

Remember, switching does nothing on its own! Make sure you install a mod from above.

1. Use `/switchy list` to see your current presets

2. use `/switchy rename default [name]` to give your starting preset a name

3. `/switchy new [name]` will create and switch to a new preset

4. `/switchy set [name]` or `/switch [name]` will switch between existing presets

### How do I change a preset nickname? or skin?

Switch to the module you'd like to change things for, then just do it as normal! <br/>
`/nick` for drogtor nicknames, `k` for fabric tailor skins, etc.<br/>
When you switch away, they'll be saved - and when you switch back, they'll be restored.

### Customize your modules

When a module is **Enabled**, it makes things "switch" (load and save) per-preset.

`/switchy module enable/disable [name]` will toggle this for your presets.

`/switchy module help` will tell you about a module, and what enabling it does.

### UI

As of `2.0.0`, installing Switchy on the client as well as the server provides you with a UI, defaulted to semicolon (`;`).

This provides a visual preview of all your presets, and the ability to click to switch between them.

![image](https://user-images.githubusercontent.com/55819817/224468718-55137a82-8269-4ce6-9bb5-0c5ce8322a68.png)

It also provides the management screen, where you can perform all the functions specified above in a visual way.

![image](https://user-images.githubusercontent.com/55819817/224468651-95630575-72dd-4a8b-b59d-371d1f5ae86d.png)

![image](https://user-images.githubusercontent.com/55819817/224468657-98da4200-3a7c-43fc-8ade-b39e4a96abc8.png)

Additionally, you can import and export your presets to a file for use on other servers/worlds.

![image](https://user-images.githubusercontent.com/55819817/224468676-8d539912-6fee-4792-b578-544dc10849af.png)

### Command-based Import/Export

Instead of the UI, you can also use commands:

`/switchy_client export` will export all of your presets and modules to a file.

`/switchy_client import [filename] [exclude] ` will import all *allowed* modules, except those in `[exclude]`

`/switchy_client import [filename] [exclude] [operator]` will import all *allowed* modules, except those in `[exclude]`,
adding those in `[operator]` if you're a server operator.

You can use `~` to specify no modules.

## Module Editing Permissions

Switchy doesn't and will not support permissions on its basic commands, and has no way to enable or disable modules
server-wide.

However, you can minorly configure which players can import module data in `/config/switchy/config.toml`

Modules will be listed with one of four import settings:

- `ALLOWED`: Importable by any player - can be changed to `OPERATOR` (e.g. origins)
- `OPERATOR`: Importable by operators when specified - can be changed to `ALLOWED` (e.g. inventories)
- `ALWAYS_ALLOWED`: Importable by any player - can't be changed (e.g. nicknames/skins)
- `NEVER`: Can't be imported due to technical limitations - can't be changed

## Developers

Switchy can be added to your project using `modCompileOnly "folk.sisby:switchy-core:x.x.x"` and these repos:

```
maven { // Switchy
    url = 'https://maven.proxyfox.dev/'
    content {
        includeGroup 'folk.sisby'
    }
}
maven { // Lib39
    url 'https://repo.sleeping.town'
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
Check out [Plural Respect](https://pluralrespect.github.io).

### Fabric?

Switchy is inescapably a plural mod. It's motivated and maintained by a plural system.

Unlike the Quilt development community, the fabric development community is not safe for plural systems. Try moving your
modlist to Quilt/QSL!

### Afterword

All mods are built on the work of many others.

![Created for ModFest: Singularity](https://user-images.githubusercontent.com/55819817/224468033-99e9fc9c-c2e2-4ed3-8a9e-ba1d9d52f3cd.svg)

We made this mod (up to v1.2.1) for Modfest: Singularity! However, we intend to maintain this mod into the future.

This mod is included in [Tinkerer's Quilt](https://modrinth.com/modpack/tinkerers-quilt) - our modpack about ease of
play and self-expression.

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.


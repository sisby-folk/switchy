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
- Player Origin with [Origins](https://modrinth.com/mod/origins/versions) (includes all layers,
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

<iframe width="896" height="504" src="https://www.youtube.com/embed/gkOGZUJOtR4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## Quick Start Guide

Remember, switching does nothing on its own! Make sure you install a mod from above.

1. Use `/switchy list` to see your current presets

2. use `/switchy rename default [name]` to give your starting preset a name

3. `/switchy new [name]` will create and switch to a new preset

4. `/switchy set [name]` or `/switch [name]` will switch between existing presets

### Customize your modules

When a module is **Enabled**, it makes things "switch" (load and save) per-preset.

`/switchy module enable/disable [name]` will toggle this for your presets.

`/switchy module help` will tell you about a preset, and what enabling it does.

### Import/Export

These commands require switchy to also be installed on the client.

`/switchy_client export` will export all of your presets and modules to a file.

You can then move to another server or singleplayer world.

`/switchy_client import [filename]` will import all *allowed* modules (see below).

`/switchy_client import [filename] [exclude] [operator]` will import all allowed modules, except modules in `[exclude]`,
plus any modules in `[operator]` if you have OP level 2. You can use `~` to specify no modules.

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
    content {
        includeGroup 'com.unascribed'
    }
}
maven { // Server Translations API
    url 'https://maven.nucleoid.xyz/'
    content {
        includeGroup 'fr.catcore'
    }
}
```

If you want to test with switchy locally, add `modLocalRuntime`.<br/>
if you're making an addon, just use `modImplementation`.<br/>

`switchy-core` includes commands and the API. <br/>
`switchy-client` enables import/export commands and a client API. <br/>
`switchy-ui` provides the client-side switch and manage screens, and module-displaying API. <br/>
`switchy-cardinal` provides data-driven CCA switchy modules and an API. <br/>
`switchy-compat` provides the built-in modules for drogtor etc. <br/>
`switchy-compat-ui` adds ui support to compat. <br/>

Adding new Modules allows more data to be switched per-preset. They only need to:

- Load and Save their data using NBT.
- Save their data from the player
- Load their data to the player

Just implement `SwitchyModule` and register it with `SwitchyModuleRegistry` using `SwitchyEvents.Init` -
See [Switchy Inventories](https://github.com/sisby-folk/switchy-inventories) for an example.

Switchy also includes am API for all its basic operations, as well as an events API for tracking switches and the
current preset.

### Data-Driven CCA Modules

If your mod uses the [Cardinal Components API](https://github.com/OnyxStudios/Cardinal-Components-API) to store its
player/entity data, you can instead register a module using an instance of `CardinalSerializerModule`.

If your component performs all of its necessary sync logic within writeToNbt/readFromNbt (or has none) - you can instead
use the static `register` method or even define the module in data.

Any data matching `data/*/switchy_cca_modules/*.json` will be
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

![Created for ModFest: Singularity](https://blob.jortage.com/blobs/5/d4d/5d4d14d96db2e2024d87cf5606cb7ce6421633a002e328947f85d210ba250ecb9f86de8df210dd031be2d4eafb0980494e7a1e8e99590a550abaa42d82768b9f)

We made this mod (up to v1.2.1) for Modfest: Singularity! However, we intend to maintain this mod into the future.

This mod is included in [Tinkerer's Quilt](https://modrinth.com/modpack/tinkerers-quilt) - our modpack about ease of
play and self-expression.

We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.


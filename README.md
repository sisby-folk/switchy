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
  - [Styled Nicknames](https://modrinth.com/mod/styled-nicknames) (Note: Switchy force-allows nickname self-assignment)
- Player Skin with [Fabric Tailor](https://modrinth.com/mod/fabrictailor)
- Player Origin with [Origins](https://modrinth.com/mod/origins/versions) (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures))
  - [Contributed by [MerchantPug](https://github.com/MerchantPug)] Apoli power state - e.g. Origin power inventories, cooldowns
- Player height and size with [Pehkui](https://modrinth.com/mod/pehkui)
- Detailed player profiles for conventions with [Lanyard](https://modrinth.com/mod/lanyard)

More functionality can be added with these Addons:
- [Switchy Inventories](https://modrinth.com/mod/switchy-inventories) - separate inventories, ender chests, and trinkets (all disabled by default)
- [Switchy Teleport](https://modrinth.com/mod/switchy-teleport) - separate player position and spawn points (all disabled by default)

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

### Import/Export

These commands require switchy to also be installed on the client.

`/switchy export` will export all of your presets and modules to a file.

You can then move to another server or singleplayer world.

`/switchy import [filename]` will import all *allowed* modules (see below).

`/switchy import [filename] [exclude] [operator]` will import all allowed modules, except modules in `[exclude]`, plus any modules in `[operator]` if you have OP level 2. You can use `~` to specify no modules.

## Import Configuration

Switchy doesn't and will not support permissions on its basic commands, and has no way to enable or disable modules server-wide.

However, you can minorly configure which players can import module data from their client in `/config/switchy/config.toml`

Modules will be listed with one of four import settings:

- `ALLOWED`: Importable by any player - can be changed to `OPERATOR` (e.g. origins)
- `OPERATOR`: Importable by operators when specified - can be changed to `ALLOWED` (e.g. inventories)
- `ALWAYS_ALLOWED`: Importable by any player - can't be changed (e.g. nicknames/skins)
- `NEVER`: Can't be imported due to technical limitations - can't be changed

## Developers

Switchy modules provide switchy with all of its practical functionality, but despite this, they're only required to do four things:
- Save themselves to NBT
- Load themselves from NBT
- Save data to themselves from the player
- Load data from themselves to the player

Because mods (and minecraft) often have to do these things already, modules can be extremely simple.

If you'd like to develop your own addon module, feel free to use [Switchy Inventories](https://github.com/sisby-folk/switchy-inventories) as an example.

To make a module, just implement `PresetModule` and register it using `PresetModuleRegistry`.

There's also an API for basic operations like getting the name of a player's presets, and switching to a specific preset.

### Data-Driven CCA Modules

If your mod uses the [Cardinal Components API](https://github.com/OnyxStudios/Cardinal-Components-API) to store its player/entity data, you can instead register a module using an instance of `CardinalSerializerCompat`.

If your component performs all of its necessary sync logic within writeToNbt/readFromNbt (or has none) - you can instead use the static `register` method or even define the module in data.

Any data matching `data/*/switchy_cca_modules/*.json` will be loaded [like so](https://github.com/sisby-folk/switchy/blob/1.19/src/main/resources/data/switchy/switchy_cca_modules/lanyard_compat.json):

 - Top level key: Cardinal component ID
   - `default`: boolean, module is enabled for players by default.
   - `importable`: See import configuration above.
   - `ifModLoaded`: a mod ID to check before trying to register the module.

## Further Info

This mod is primarily motivated by improving accessibility for [plural systems](https://morethanone.info).<br/>
Check out the [Plural Respect Document](https://bit.ly/pluralrespect).

### Fabric?

Switchy is inescapably a plural mod. It's motivated and maintained by a plural system.

Unlike the Quilt development community, the fabric development community is not safe for plural systems. Try moving your modlist to Quilt/QSL!

### Afterword

All mods are built on the work of many others.

![Created for ModFest: Singularity](https://blob.jortage.com/blobs/5/d4d/5d4d14d96db2e2024d87cf5606cb7ce6421633a002e328947f85d210ba250ecb9f86de8df210dd031be2d4eafb0980494e7a1e8e99590a550abaa42d82768b9f)

We made this mod (up to v1.2.1) for Modfest: Singularity! However, we intend to maintain this mod into the future.

This mod is included in [Tinkerer's Quilt](https://modrinth.com/modpack/tinkerers-quilt) - our modpack about ease of play and self-expression.



We're open to suggestions for how to implement stuff better - if you see something wonky and have an idea - let us know.


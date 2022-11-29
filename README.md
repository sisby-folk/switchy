<p align="center"><img alt="switchy banner" src="https://user-images.githubusercontent.com/55819817/198210616-eb37be12-cd96-40c8-a941-68a96b2aadfc.png" /></p>

<p align="center">An extensible preset system for player customizations provided by other mods.<br/>
Works in singleplayer and on server-side.</p>

---


## What is Switchy?

Switchy lets you use commands make ***presets*** that are stored with your player data.

Switchy will load ***modules*** that tell presets what to store.

When you ***switch*** presets, your data is saved to the old preset, then loaded from the new one.

Modules define what this data is: items, nicknames, player skins, player position, you name it.
They don't even have to implement functionality (like nicknames) themselves - they can just interact with an existing mod.

## How do you use it?

First, install a vanilla add-on or any compatible mod (see modules).

Use `/switchy list` any time to see your current presets.

Then, use `/switchy rename default [name]` to give your starting preset a name.

`/switchy new [name]` will create and switch to a new preset.

`/switchy set [name]` or `/switch [name]` will switch between existing presets

When a module is **Enabled**, it makes things "switch" (load and save) per-preset.
Using `/switchy module enable/disable [name]` toggles this behaviour for your own presets.

Modules cannot (and will never) be able to be enabled or disabled server-wide.

For more commands, including `export` and `import` (client required), type `/switchy help`

## Modules

Most modules require installing another mod to work! Be sure to follow the links below.

Switchy itself comes packaged with modules for:
- [Drogtor The Nickinator](https://modrinth.com/mod/drogtor) - player `nickname`, `bio`, and `color`
- [Styled Nicknames](https://modrinth.com/mod/styled-nicknames) - nicknames. Recommended config WIP
  - Note: Using Switchy with Styled Nicknames disables permissions for self-assigning nicknames
- [Fabric Tailor](https://modrinth.com/mod/fabrictailor) - player skin
- [Origins](https://modrinth.com/mod/origins/versions) - current origins (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures) for player height)
  - [Apoli](https://github.com/apace100/apoli) (by [MerchantPug](https://github.com/MerchantPug)) - current power state (e.g. Inventories, Resources/Cooldowns)
- [Pehkui](https://modrinth.com/mod/pehkui) - pehkui `width` and `height` properties.
- [Lanyard](https://modrinth.com/mod/lanyard) - lanyard name, pronouns, and bio.

You can add more modules from these first-party add-ons:
- [Switchy Inventories](https://modrinth.com/mod/switchy-inventories) - addon for minecraft.
  - Modules for separate inventories, ender chests, and trinket inventories.
  - All modules are disabled by default.

.. And these third-party add-ons!:
- [Switchy Teleport](https://modrinth.com/mod/switchy-teleport) - addon for minecraft.
  - Modules for keeping player position and spawn point separate between presets.
  - All modules are disabled by default.


## Showcase

<iframe width="896" height="504" src="https://www.youtube.com/embed/gkOGZUJOtR4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## Developers

Switchy modules provide switchy with all of its practical functionality, but despite this, they're only required to do four things:
- Save themselves to NBT.
- Load themselves from NBT.
- Save data to themselves from the player.
- Load data from themselves to the player.

Because mods (and minecraft) often have to do these things already, modules can be extremely simple.

If you'd like to develop your own addon module, feel free to use [Switchy Inventories](https://github.com/sisby-folk/switchy-inventories) as an example.

To make a module, just implement `api.PresetModule` and register it using `api.PresetModuleRegistry`.

There's also an API for basic operations like getting the name of a player's presets, and switching to a specific preset.

## Further Info

All mods are built on the work of many others.

![Created for ModFest: Singularity](https://blob.jortage.com/blobs/5/d4d/5d4d14d96db2e2024d87cf5606cb7ce6421633a002e328947f85d210ba250ecb9f86de8df210dd031be2d4eafb0980494e7a1e8e99590a550abaa42d82768b9f)

We made this mod (up to v1.2.1) for Modfest: Singularity! However, we intend to maintain this mod into the future.

This mod is included in [Tinkerer's Quilt](https://modrinth.com/modpack/tinkerers-quilt) - our modpack about ease of play and self-expression.

This mod is primarily motivated by improving accessibility for [plural systems](https://morethanone.info).
Check out the [Plural Respect Document](https://bit.ly/pluralrespect).

We're always open to suggestions for how to implement code snippets better - if you see a wonky method and have an idea - let us know.

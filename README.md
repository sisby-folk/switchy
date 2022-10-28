<p align="center"><img alt="switchy banner" src="https://user-images.githubusercontent.com/55819817/198210616-eb37be12-cd96-40c8-a941-68a96b2aadfc.png" /></p>

<p align="center">An extensible preset system for player customizations provided by other mods.<br/>
Works in singleplayer and on server-side.</p>

---


## What is Switchy?

Switchy is a mod that lets you make presets using commands.
The presets don't do anything.

Thankfully, Switchy is loaded with modules that make presets do things. Mostly things from other mods.

## How do you use it?

First, install a vanilla add-on or any compatible mod (see modules).

Use `/switchy list` to see your current presets.

Then, use `/switchy rename default [name]` to give your starting preset a name.

`/switchy new [name]` will create and switch to a new preset.

`/switchy set [name]` or `/switch [name]` will switch between existing presets

When a module is **Enabled**, it makes things "switch" per-preset.
Using `/switchy module enable/disable [name]` allows you to toggle this behaviour for just your presets.

Switchy provides no ability for server owners to enable or disable modules server-wide.

For more commands, type `/switchy help`

## Modules

Switchy itself comes packaged with modules for:
- [Drogtor The Nickinator](https://modrinth.com/mod/drogtor) - player `nickname`, `bio`, and `color`
- [Fabric Tailor](https://modrinth.com/mod/fabrictailor) - set skin
- [Origins](https://modrinth.com/mod/origins/versions) - current origins (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures) for player height)
- [Pehkui](https://modrinth.com/mod/pehkui) - pehkui `width` and `height` properties.
- (1.19) [Lanyard](https://modrinth.com/mod/lanyard) - lanyard name, pronouns, and bio.

You can add more modules from these first-party add-ons:
- [Switchy Inventories](https://modrinth.com/mod/switchy-inventories) - addon for minecraft.
  - Allows for separate inventories, ender chests, and trinket inventories.
  - Each module is disabled by default, allowing players to enable them individually.

.. And these third-party add-ons!:
- [Switchy Teleport](https://modrinth.com/mod/switchy-teleport) - addon for minecraft.
  - Modules for keeping player position and spawn point separate between presets. 
  - Both modues are disabled by default. 
 

## Showcase

<iframe width="896" height="504" src="https://www.youtube.com/embed/gkOGZUJOtR4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## Design - Modules

Switchy, at its core, is a mod that gives you a list of empty presets. You can add to the list, set what preset is "current", and so on, but none of these presets *do* anything.

> At its core - Switchy is a mod that **does nothing** - and that's key to its design.

Switchy modules provide switchy with all of its practical functionality, but despite this, they're only required to do four things:
 - Save themselves to an NBT Compound.
 - Load themselves from an NBT Compound.
 - Read data to themselves from the player.
 - Apply data from themselves to the player.

Because mods, and minecraft itself, often have to do these things already, compatibility modules are often extremely simple to write (the *Switchy Inventories* module took **[5 lines of code](https://github.com/sisby-folk/switchy-inventories/blob/1.18/src/main/java/folk/sisby/switchy_inventories/compat/InventoryCompat.java)**.)

We hope players and developers find this useful.

## Developing Addons

If you'd like to develop your own addon module, feel free to clone the [Inventories Module](https://github.com/sisby-folk/switchy-inventories) to get started.

Otherwise, all an addon mod needs to do is create a module class that implements `api.PresetModule` and registers itself using the `api.PresetModuleRegistry` - Go wild!

## Further Info

All mods are built on the work of many others.

![Created for ModFest: Singularity](https://blob.jortage.com/blobs/5/d4d/5d4d14d96db2e2024d87cf5606cb7ce6421633a002e328947f85d210ba250ecb9f86de8df210dd031be2d4eafb0980494e7a1e8e99590a550abaa42d82768b9f)

We made this mod (up to v1.2.1) for Modfest: Singularity! However, we intend to maintain this mod into the future.

This mod is included in [Tinkerer's Quilt](https://modrinth.com/modpack/tinkerers-quilt) - our modpack all about ease of play and ease of expression.

This mod is primarily motivated by improving accessibility for [plural systems](https://morethanone.info).
We support the [Plural Respect Document](https://bit.ly/pluralrespect) - its perspectives largely reflect our own.

We're primarily modpack developers - not mod developers! If you want to port this mod, do it yourself!

Though feel free to let us know if we can spruce anything on the implementation side - PRs and issues with code snippets are welcome as long as you can help us understand them.

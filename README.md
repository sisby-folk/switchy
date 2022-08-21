<p align="center"><img alt="switchy banner" src="https://user-images.githubusercontent.com/55819817/185277937-60b55666-07b9-46d3-881b-0f45ea39fb73.png" /></p>

<p align="center">An extensible preset system for player customizations provided by other mods.<br/>
Works in singleplayer and on server-side.</p>

---

Commands:
- `/switchy help`
- `/switchy list` - list your created presets, and the current one.
- `/switchy new [name]` - create a new preset from your active customizations.
- `/switchy set [name]` - switch to another preset.
- `/switch [name]` - alias for `/switchy set`
- `/switchy rename [name] [name]` - rename a preset.
- `/switchy delete [name]` - delete a preset.
- `/switchy module enable [name]` - enable a module for you.
- `/switchy delete disable [name]` - disable a module for you.

On its own, supports player customization from:
- [Drogtor The Nickinator](https://modrinth.com/mod/drogtor) - player `nickname`, `bio`, and `color`
- [Fabric Tailor](https://modrinth.com/mod/fabrictailor) - set skin
- [Origins](https://modrinth.com/mod/origins/versions) - current origins (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures) for player height)

Further modules can be added from these addons:
- [Switchy Inventories](https://modrinth.com/mod/switchy-inventories) - Addon for minecraft. Gives each preset a separate inventory.
  - Highly recommended - Comes disabled by default and can be enabled on a per-player basis.

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

If you'd like to develop your own addon module, feel free to fork the [Inventories Module](https://github.com/sisby-folk/switchy-inventories) to get started.

Otherwise, all an addon mod needs to do is create a module class that implements `api.PresetModule` and registers itself using the `api.PresetModuleRegistry` - Go wild!

## Further Info

All mods are built on the work of many others.

We're primarily modpack developers - not mod developers! If you want to port this mod, do it yourself!

Though feel free to let us know if we can spruce anything on the implementation side - PRs and issues with code snippets are welcome as long as you can help us understand them.

# Switchy

Adds an extensible preset system that allows players to quickly switch between player customizations from other mods.

![image](https://user-images.githubusercontent.com/55819817/185186588-6bd80141-727c-4ed3-a987-815aab17d790.png)


Works in singleplayer and on server-side.

Commands:
 - `/switchy help`
 - `/switchy list` - list your created presets, and the current one.
 - `/switchy new [name]` - create a new preset from your active customizations.
 - `/switchy set [name]` - switch to another preset.
 - `/switch [name]` - alias for `/switchy set`
 - `/switchy delete [name]` - delete a preset.

Support player customization from:
 - [Drogtor The Nickinator](https://modrinth.com/mod/drogtor) - player `nickname`, `bio`, and `color`
 - [Fabric Tailor](https://modrinth.com/mod/fabrictailor) - set skin
 - [Origins](https://modrinth.com/mod/origins/versions) - current origins (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures) for player height)

## Design

Switchy can be loaded with or without any of its compatible mods, and any mod can add additional compat modules by adding them to `Switchy.COMPAT_MODULES`. Only loaded modules will save/load data.

Note that this means that removing a compatible mod will cause its data to be removed from presets.

## Further Info

All mods are built on the work of many others.

We're primarily modpack developers - not mod developers! If you want to port this mod, do it yourself!

Though feel free to let us know if we can spruce anything on the implementation side - PRs and issues with code snippets are welcome as long as you can help us understand them.

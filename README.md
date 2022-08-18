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
 - `/switchy delete [name]` - delete a preset.

On its own, supports player customization from:
 - [Drogtor The Nickinator](https://modrinth.com/mod/drogtor) - player `nickname`, `bio`, and `color`
 - [Fabric Tailor](https://modrinth.com/mod/fabrictailor) - set skin
 - [Origins](https://modrinth.com/mod/origins/versions) - current origins (includes all layers, e.g. [Statures](https://modrinth.com/mod/tinkerers-statures) for player height)

## Design

Switchy handles all of its interactions with other mods using a **Compatibility Module** system. Any mod can add new compatibility modules to switchy, and it will work without any changes. Switchy's mod compatibility for the mods above are made in this format, and can be used as reference.

Only loaded modules will load and save data - Note that this means that removing a compatible mod will cause its data to be removed from presets.

## Further Info

All mods are built on the work of many others.

We're primarily modpack developers - not mod developers! If you want to port this mod, do it yourself!

Though feel free to let us know if we can spruce anything on the implementation side - PRs and issues with code snippets are welcome as long as you can help us understand them.

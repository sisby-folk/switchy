<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center><img alt="mod preview" src="https://github.com/user-attachments/assets/213a47e5-55d1-4f46-a109-c1a05f47ced4"/></center>

<center>
Serverside player profiles for hotseat multiplayer, adventure maps, and plural systems.<br/>
<b>Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on (neo)forge.<br/></b>
</center>

---

> _**Disclaimer**: All mods that manipulate player data have a save corruption risk!<br/>
Always take backups of your worlds, especially when updating mods._

---

**Switchy** allows players to create **profiles**, which have their own partial player data, and can be switched between.

Profile data is defined by player-toggleable _components_, including:
- Vanilla health, hunger, status effects, inventory, ender chest, location, spawn point, and xp
- [Fabric Tailor](https://modrinth.com/mod/fabrictailor) skins
- [Styled Nicknames](https://modrinth.com/mod/styled-nicknames) nicknames
- [Trinkets](modrinth.com/mod/trinkets) slots
- [Origins](https://modrinth.com/mod/origins) origins

This means you can adjust profiles to only contain some of the above, e.g. just nicknames, skins, and origins.

## Usage

Switchy utilizes a command-based text interface with tooltips and clickable text. 

Running `/switchy help` will provide a list of top-level commands, but in short:
- Run `/switchy` to edit and switch between profiles
- Run `/switchy components` to toggle components
- Run `/switchy import [url].json` to import profiles from a Switchy/[PK](https://pluralkit.me/guide/#exporting-your-pluralkit-data) export
- Run `/switchy export` to export name/skin data to the clipboard (upload somewhere to import)
  - Switchy exports can also be created/edited using [Utter](https://utter.y2k.diy/), which preserves exported skin data.

## Compatibility

Switchy can be used to switch modded data in a modpack by setting up **switchy components** in a datapack:

```json5
// data/minecraft/switchy_components/inventory/ender_chest.json (minecraft:inventory/ender_chest)
{
  "enabled": true, // whether to load the component at all. use this instead of deleting default files, as they'll regenerate.
  "codec": "inventory", // which codec (from the registry in SwitchyComponentTypes) to use to deserialize the data. defaults to "nbt" (passthrough)
  "path": "EnderItems", // NBT path targeting the part of player.dat to load from / modify to
  "preview": "inventory", // which text previewer (from the registry in SwitchyComponentTypes) to use in chat. null = toString(), use "trunc" for long data. Start with $ to use an NBT path for passthrough components.
  "prefix": "👁 ", // a prefix to add to the text preview, for glanceability
  "emptyChecker": "inventory", // which empty checker (from the registry in SwitchyComponentTypes) to use to prevent profile deletion for precious data. Start with $ to use an NBT path for passthrough components.
  "group": "inventory", // components with a matching group ID will be previewed and toggled as if they're one component
  "default": [], // default value. JSON serialized. set to "$copy" to copy the value from previous. set to null or omit to delete the key from player data as the initial value.
  "hidden": false // hides the component preview in the profiles list (still shown in the components list)
}
```

Feel free to browse the [included components](https://github.com/sisby-folk/switchy/tree/1.20/src/main/resources/data) for examples.

You're welcome to PR datapack components (with load conditions), along with any additional previewers etc.

Switching data outside player NBT requires an addon. API TBD - Interested addon devs can hit up the [issues page](https://github.com/sisby-folk/switchy/issues).

## Afterword

If you're a plural system (or a friend to one) and appreciate our work, please consider reading and sharing [sys.guide](https://sys.guide), our plurality handbook.

Switchy was our first original minecraft mod, made during the mod jam [ModFest: Singularity](https://modfest.net/singularity/submissions).<br/>
Since making it, the minecraft modding community has given us friendship, mentorship,<br/>
endless favours, an outlet for self-expression, and reinforced our passion for software as an art.<br/>
Everyone who's been a part of that - and you should know who you are - thank you.

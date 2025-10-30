<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center><img alt="mod preview" src="https://github.com/user-attachments/assets/b51c7392-bcf5-4c3f-9088-d35933636dc4"/></center>

<center>
Serverside player profiles for hotseat multiplayer, adventure maps, and plural systems.
</center>

---

**Switchy** allows players to create **profiles**, which have their own partial player data, and can be switched between using commands.

Data components are player-toggleable and config-driven, including:
- Vanilla Health, Food, Inventory, Ender Chest, Position, and XP
- [Fabric Tailor](https://modrinth.com/mod/fabrictailor) skins
- [Styled Nicknames](https://modrinth.com/mod/styled-nicknames) nicknames
- [Trinkets](modrinth.com/mod/trinkets) slots
- [Origins](https://modrinth.com/mod/origins) origins

## Usage

Use `/switchy` to view your profiles, alongside clickable shortcuts for `switch`, `edit id/name`, and `delete`.

Use `/switchy components` to view your switched components, alongside shortcuts for `enable` and `disable` for per-player customization.

Use `/switchy import [url].json` to import profiles from a [pluralkit export](https://pluralkit.me/guide/#exporting-your-pluralkit-data) (better with [Styled Nicknames](https://modrinth.com/mod/styled-nicknames)).

Note that on **1.20 and below**, switching profiles requires manually reconnecting to the server / reloading the save, making it incompatible with `Open To LAN` worlds.

## Compatibility

Components are defined by json files in `config/switchy/components`.

Mod presence is checked by file location, so `components/origins/origin.json` creates an `origins:origin` component if `origins` is loaded.

```json5
// config/components/enderchest.json (minecraft:enderchest)
{
  "enabled": true, // whether to load the component at all. use this instead of deleting default files, as they'll regenerate.
  "codec": "inventory", // which codec (from the registry in SwitchyComponentTypes) to use to deserialize the data. defaults to "nbt" (passthrough)
  "path": "EnderItems", // NBT path targeting the part of player.dat to load from / modify to
  "preview": "inventory", // which text previewer (from the registry in SwitchyComponentTypes) to use in chat. null = toString(), use "trunc" for long data.
  "prefix": "👁 ", // a prefix to add to the text preview, for glanceability
  "emptyChecker": "inventory" // which empty checker (from the registry in SwitchyComponentTypes) to use to prevent profile deletion for precious data.
}
```

Feel free to PR additional generic codecs, previewers, and empty checkers.

Switching data that isn't stored in player NBT (as is done for `switchy:name` for Styled Nicknames compat) is not possible without a custom addon.<br/> 
API TBD - Let us know if you're a mod developer seeking API features for integration on the [issues page](https://github.com/sisby-folk/switchy/issues).

## Afterword

If you're a plural system (or a friend to one) and appreciate our work, please consider reading and sharing [sys.guide](https://sys.guide), our plurality handbook.

Switchy was our first original minecraft mod, made during the mod jam [ModFest: Singularity](https://modfest.net/singularity/submissions).<br/>
Since making it, the minecraft modding community has given us friendship, mentorship,<br/>
endless favours, an outlet for self-expression, and reinforced our passion for software as an art.<br/>
Everyone who's been a part of that - and you should know who you are - thank you.

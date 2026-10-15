# Settings

How a mod on J's Core gives players and server owners settings: each one declared once with its range, written to
a file with its explanation, pulled back into its range when someone writes a wrong value, changed on a settings
screen drawn in the player's style, shared as text and as presets, and changed on a server from an operator's own
game.

## A setting

*Added 2026-06-04; more kinds and traits 2026-10-15.*

```java
public static final ConfigKey<Integer> KILN_HEAT = ConfigKey.whole("kiln.max_heat", 1_200)
        .range(100, 5_000)
        .comment("How hot a kiln can get, in degrees.")
        .named("Highest kiln heat")
        .unit("degrees");
```

- The path is dotted: the last part is the setting's name, the parts before it are the sections it sits in
  (`[kiln]` in the file).
- The kinds: `ConfigKey.flag` (yes or no), `whole` (a whole number), `wholeLong`, `number` (a decimal), `text`,
  `colour` (written `#RRGGBB`, and only that), `ids` (a list of ids of items, blocks or fluids), and
  `ConfigKey.of(path, codec, default)` for anything a codec writes (a list, a map, a record).
- `range(min, max)` bounds a number; the default must be inside it, and the bounds must be of the same type as the
  default (`100, 5_000` for an `Integer`, `0.0, 1.0` for a `Double`). `allowing("low", "high")` limits a text to
  those words. A setting has one or the other, not both.
- `comment` is written above the setting in the file and is its description on the screen; the range or the words
  allowed, and the default, are added to the file's comment for you.
- `named` is the setting's name on the settings screen, in English. It is required for a TOML file's settings; a
  setting of another format is on the screen only when it has one.
- `unit` (*added 2026-10-06*) is what a number counts, in English (`"KB/s"`, `"ticks"`, `"days"`): the screen
  writes it after the number and after its default and bounds, and it is translated like the name.

What the screen shows beside the value (*added 2026-10-15*):

| Trait | What it does |
| --- | --- |
| `appliesOn(ConfigApply.RELOAD)` / `RESTART` | Says when a change takes hold: the screen marks the setting "on reload" or "needs a restart", and counts the changes that wait for a restart. `NOW` is the default. |
| `activeWhen(otherFlag)` / `activeWhen(otherWord, "a", "b")` | The setting is greyed, and its control locked, while the other setting of the same file is off (or is not one of the words). The screen says "Active when ... is on" under its name. |
| `shownWhen(otherFlag)` / `shownWhen(otherWord, ...)` | The same, but the setting is left off the screen instead of greyed. |
| `pictured("mymod:textures/gui/config/veins.png", frames)` | A picture beside the description; a texture made of `frames` frames one under the other is shown in turn, half a second each. |
| `inManual("jscore:technical_reference", "veins")` | The description ends in "Read it in the manual", a link to that entry; closing the manual comes back to the settings. |
| `shownAs(ConfigControl.BLOCK)` | The control, where the kind of value does not say: `COLOUR`, `ITEM`, `BLOCK` or `FLUID` for one id, `ITEMS`, `BLOCKS` or `FLUIDS` for a list of them. |
| `choosing("mymod:chooser")` | A screen of its own picks the value (see `ConfigChoosers`). |

## A file of settings

*Added 2026-10-01; rules and presets 2026-10-15.*

```java
public static final ConfigFile FILE = ConfigFile.builder("mymod-server", ConfigSide.SERVER, ConfigFormats.TOML)
        .comment("How the kilns of My Mod behave.")
        .sectionNamed("kiln", "Kilns")
        .key(KILN_HEAT)
        .key(KILN_LOW)
        .rule(LOW_UNDER_HIGH, values -> values.get(KILN_LOW) <= values.get(KILN_HEAT))
        .preset(ConfigPreset.of("gentle", GENTLE).with(KILN_HEAT, 800))
        .build();

// in your mod's constructor:
ConfigFiles.register(FILE, modEventBus, modContainer);

// anywhere:
int heat = FILE.get(KILN_HEAT);
```

**Where a file lives** is its side:

| Side | Where | For |
| --- | --- | --- |
| `CLIENT` | the player's `config` folder | one player's own taste; never sent anywhere |
| `COMMON` | the `config` folder of every game | the same on the server and every player's game |
| `SERVER` | `serverconfig` inside each world | one world's rules, set by its owner, sent to every player |

**The format** is TOML (handed to NeoForge, which writes it and reloads it when it changes), or JSON, JSON5, YAML or
NBT (kept by the Core, written safely so a crash mid-write never leaves half a file). TOML, JSON5 and YAML keep their
comments; JSON cannot.

`sectionNamed` names each section and group on the settings screen; a TOML file whose section has no name refuses to
build, as does a setting without `named`, because the screen and the language file need them. `sectionComment` says
what a section is for. `version(n)` and `upgrade(from, step)` change an older file's shape when it is read
(`IConfigUpgrade.rename(from, to)`, `remove`).

**A rule** between settings (*added 2026-10-15*) is a sentence (a `TextKey`) and a test of the values in hand. A file
read with a rule broken says so in the log; a settings screen whose changes break one says it over the settings and
will not keep them until they are put right.

**A preset** (*added 2026-10-15*) is a named set of values for some of the file's settings, which you ship with the
file; picking one changes only what it names. People can save their own too (see below).

## Being told a setting changed

*Added 2026-10-15.*

```java
FILE.whenChanged(KILN_HEAT, heat -> Kilns.setLimit(heat));
FILE.onAnyChange(key -> Kilns.refresh());
```

Each is told once for every change of value, wherever it came from: the file read again, the settings screen, a
server's values arriving on a player's game, or a command. A value set to what it already was tells nobody.
`key(..., use)` on the builder is still the way to have a value handed over each time the file is read.

## When someone writes a wrong value

*Added 2026-06-04.*

Every value is checked when the file is read, and a wrong one never reaches your code:

- a number outside its range is pulled to the nearer end, with a warning in the log;
- a word not among the allowed ones, or a value that cannot be read, becomes the default;
- a list or map that is partly wrong keeps the part that reads.

A file that needed any of that, or that misses settings or has unknown ones, is written back whole, with today's
comments. A file written by a **newer** version of your mod is read as far as possible and never written over.

## Presets, text, and a modpack's defaults

*Added 2026-10-15.*

- **Presets people make** are files of the same format as the settings, in `config/jstech-presets/<file>/`, saved from
  the settings screen's Presets... (`ConfigPresets.save`, `load`, `saved`, `delete`). `ConfigPresets.find(file, name)`
  finds a shipped preset first, then a saved one.
- **Settings as text**: Export on the screen writes the open file's values in the file's own format (JSON for an NBT
  file), to copy and share; Import takes text back, in any of the text formats, and holds what it read as changes
  to keep with Done (`ConfigPresets.export`, `importText`).
- **A modpack's defaults**: a world's settings file that does not exist yet starts as the copy in the game's
  `defaultconfigs` folder, as NeoForge does for its own TOML files. Put `defaultconfigs/mymod-server.json5` in the
  pack and every new world starts with it, whatever the file's format.

## A server's settings from afar

*Added 2026-10-15.*

A world's settings are sent to every player as they join, and again at the end of the tick in which one changes, so
each player's game shows and uses the server's values; leaving the server puts them back to their defaults. An
operator (level 2) changes them on the settings screen of their own game: Done sends the changes to the server, which
checks the permission, keeps them, writes the file and sends the new values to everyone, then says in the operator's
chat how many it kept and which it refused. A player who is not an operator sees them, read only.

Operators also have commands:

| Command | What it does |
| --- | --- |
| `/jstech config get <file> <setting>` | Says a setting's value. |
| `/jstech config set <file> <setting> <value>` | Sets it for everyone; the value is written as JSON (`true`, `12`, `["a", "b"]`) or as plain words. |
| `/jstech config reset <file> <setting>` | Puts it back to its default. |
| `/jstech config preset <file> <name>` | Picks a preset, shipped or saved on the server. |

They reach a world's files and those of every game, by the file's name without its extension (`jstech-balance`).

## The settings screen

*Added 2026-10-05; redrawn 2026-10-06; rebuilt 2026-10-15.*

The Core draws a settings screen for every mod that hands it over, reached from the game's Mods list. It is a menu
over the whole game, drawn through the style the player wears: the game's own by default, its tabs, buttons, sliders,
fields and lists over the blurred world.

- **Across the top**, a tab for each kind of file you have (World, This player, Every game), and under them a banner
  when there is something to say: an operator changing a server's settings, a world's settings only read here, a
  rule broken.
- **Down the left**, the open tab's sections, each with how many settings it holds and a mark while one of them
  holds a change.
- **On the right**, the search (it looks through every tab, each section's finds under its name), the filter that
  shows only the settings off their defaults, and Presets...; then the section's settings, in groups that fold
  away (the second part of a setting's path), each with its name, its marks ("needs a restart", "on reload"), the
  setting it is active under, and its control. A setting off its default has a button back to it.
- **The controls**: a button that turns a switch on and off, a slider with the number typed beside it, a number
  stepped or typed, a drop-down list of words, a field, a colour picker, a thing picked from the game's (a window
  with a search and the matches in slots), a list's entries each with a cross and a way to add one, a map's entries
  each with its value in a field and a row to add one, or a chooser of its own.
- **The description**: the pointer resting on a setting's name shows all of it, with its picture, its default and
  bounds, and its link to the manual; a click pins it, so the link can be followed.
- **The footer**: Defaults (the open section back to its defaults), Undo and Redo, how many changes wait and how
  many of them wait for a restart, Export, Import, and Done, which keeps every change. Escape leaves without keeping
  anything.
- **The keyboard** reaches everything: Tab and Shift+Tab, or the arrows up and down, go from control to control in
  reading order; Enter or Space presses; Ctrl+Tab goes to the next tab, Ctrl+F to the search, Ctrl+Z and Ctrl+Y undo
  and redo. A game controller works through Controlify's pointer.

Give your mod the screen from its client entry point, with or without a mark of its own (a small drawing in one
colour, `IConfigBadge`, shown in the header's corner when the tabs leave room; without one it gets
`IConfigBadge.CHIP`):

```java
modContainer.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new);
// or, with your mark:
modContainer.registerExtensionPoint(IConfigScreenFactory.class,
        (mod, parent) -> new CoreConfigScreen(mod, parent, MY_MARK));
```

The names, descriptions and units come from your settings' `named`, `comment` and `unit` and your sections'
`sectionNamed`, written to your language file as `<mod>.configuration.<path>`, `<mod>.configuration.<path>.tooltip`
and `<mod>.configuration.<path>.unit`, so they can be translated. A word of a list is shown as
`<mod>.configuration.<path>.<word>` when your language file has it, and as the word otherwise.

## The Core's own settings

The Core's server settings are in `jstech-balance.toml`, in each world's `serverconfig` folder:

| Setting | Default | What it tunes |
| --- | --- | --- |
| `balance.hdd_latency_ticks`, `ssd_latency_ticks`, `nvme_latency_ticks` | 10, 3, 1 | How long each kind of disk waits before a transfer starts. |
| `balance.operation_waiting_timeout_ticks` | 1200 | How long an Operation waits on something busy before it gives up. |
| `balance.operation_priority_aging_ticks` | 600 | How long a waiting Operation waits per step of priority it climbs; 0 never climbs. |
| `balance.subframe_efficiency_factor` | 0.6 | The share of its capacity a Subframe lends its Mainframe. |
| `balance.orphaned_operations_expiry_hours` | 24 | How long a saved Operation nobody resumed may wait before a reload drops it; 0 never. |
| `balance.program_machine_micros` | 1000 | Real time, in microseconds, one machine may spend running its programs in a tick. |
| `balance.program_server_micros` | 8000 | The same for every machine of the server together. |
| `media.download_kilobytes_per_second` | 1024 | How fast the server sends recordings to each player. |
| `media.upload_kilobytes_per_second` | 512 | How fast a player sends a recording. |
| `media.max_file_megabytes` | 32 | The largest recording a player may bring; 0 takes none. |
| `media.player_quota_megabytes` | 512 | How much one player's recordings may take together; 0 no limit. |
| `calendar.days_per_season` | 28 | How many days a season lasts; a year is four seasons. |
| `world.chunks_per_owner` | 25 | How many chunks each owner may keep loaded (0 to 4,096). |
| `protection.team_damage` | false | Whether players on one team can hurt each other ([Protection](PROTECTION.md#damage-between-players)). |
| `protection.audit` | false | Whether who used, changed or broke what others own is written down. |
| `protection.audit_entries` | 10000 | How many entries the audit log keeps (100 to 1,000,000). |
| `protection.robots_per_player` | 16 | How many robots one player may have at once (0 to 1,024). |

## What can go wrong

- **The game or the data generation stops on a setting without a name.** A TOML setting has no `named`, or a
  section no `sectionNamed`.
- **A setting is refused as it is declared.** Its default is outside its range, its bounds are of another number type
  than its default, or a colour's default is not written `#RRGGBB`.
- **A preset is refused as the file is built.** It sets a setting the file does not hold.
- **A value someone set comes back changed.** It was outside its range and was pulled in; the log says so.
- **Done does nothing.** A rule between settings is broken; the banner over the settings says which.
- **Server settings cannot be changed from the screen.** Outside a world they belong to no world; on a server, only an
  operator changes them. A world's settings can also be changed with `/jstech config set`.

# Settings

How a mod on J's Core gives players and server owners settings: each one declared once with its range, written to
a file with its explanation, pulled back into its range when someone writes a wrong value, and shown on a
settings screen in the Core's look.

## A setting

*Added 2026-06-04.*

```java
public static final ConfigKey<Integer> KILN_HEAT = ConfigKey.whole("kiln.max_heat", 1_200)
        .range(100, 5_000)
        .comment("How hot a kiln can get, in degrees.")
        .named("Highest kiln heat");
```

- The path is dotted: the last part is the setting's name, the parts before it are the sections it sits in
  (`[kiln]` in the file).
- The kinds: `ConfigKey.flag` (yes or no), `whole` (a whole number), `wholeLong`, `number` (a decimal), `text`,
  and `ConfigKey.of(path, codec, default)` for anything a codec writes (a list, a map, a record).
- `range(min, max)` bounds a number; the default must be inside it, and the bounds must be of the same type as the
  default (`100, 5_000` for an `Integer`, `0.0, 1.0` for a `Double`). `allowing("low", "high")` limits a text to
  those words. A setting has one or the other, not both.
- `comment` is written above the setting in the file and shown as its tooltip on the screen; the range or the
  words allowed, and the default, are added to it for you.
- `named` is the setting's name on the settings screen, in English. It is required for a TOML file's settings.

## A file of settings

*Added 2026-10-01.*

```java
public static final ConfigFile FILE = ConfigFile.builder("mymod-server", ConfigSide.SERVER, ConfigFormats.TOML)
        .comment("How the kilns of My Mod behave.")
        .sectionNamed("kiln", "Kilns")
        .key(KILN_HEAT)
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
| `SERVER` | `serverconfig` inside each world | one world's rules, set by its owner |

**The format** is TOML (handed to NeoForge, which writes it, sends a server's file to its players, and reloads it
when it changes), or JSON, JSON5 or YAML (kept by the Core, written safely so a crash mid-write never leaves half a
file). TOML, JSON5 and YAML keep their comments; JSON cannot.

`sectionNamed` names each section on the settings screen; a TOML file whose section has no name refuses to build,
as does a setting without `named`, because the screen and the language file need them. `version(n)` and
`upgrade(from, step)` change an older file's shape when it is read (`IConfigUpgrade.rename(from, to)`, `remove`).

## When someone writes a wrong value

*Added 2026-06-04.*

Every value is checked when the file is read, and a wrong one never reaches your code:

- a number outside its range is pulled to the nearer end, with a warning in the log;
- a word not among the allowed ones, or a value that cannot be read, becomes the default;
- a list or map that is partly wrong keeps the part that reads.

A file that needed any of that, or that misses settings or has unknown ones, is written back whole, with today's
comments. A file written by a **newer** version of your mod is read as far as possible and never written over.

## The settings screen

*Added 2026-10-05.*

The Core draws a settings screen for every file of a mod, reached from the game's Mods list: the files and their
sections on the left, the settings on the right, each with the control its kind needs (a switch, a number you type
or step, a word to cycle through, a text field). Done keeps the changes and writes them, Cancel or Escape drops
them. A world's server settings can only be changed inside that world, on the game running it.

Give your mod the screen from its client entry point:

```java
modContainer.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new);
```

The names and tooltips come from your settings' `named` and `comment`, written to your language file as
`<mod>.configuration.<path>` and `<mod>.configuration.<path>.tooltip`, so they can be translated.

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

## What can go wrong

- **The game or the data generation stops on a setting without a name.** A TOML setting has no `named`, or a
  section no `sectionNamed`.
- **A setting is refused as it is declared.** Its default is outside its range, or its bounds are of another
  number type than its default.
- **A value someone set comes back changed.** It was outside its range and was pulled in; the log says so.
- **Server settings cannot be changed from the screen.** They belong to a world: open that world first. On a
  dedicated server they are changed in its file.

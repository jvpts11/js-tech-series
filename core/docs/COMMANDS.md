# Commands

Every mod of the series puts its commands under one root, `/jstech`, so a player finds them all in one place and
no two mods fight over a name.

## Declaring a command

*Added 2026-10-05.*

```java
SeriesCommands.declare("kiln", SeriesCommands.GAME_MASTERS, (node, context) -> node
        .then(Commands.argument("players", EntityArgument.players())
                .executes(MyCommands::showKilns)));
```

Call it once, from your mod's constructor. `/jstech kiln <players>` then exists on every server. The node is the
game's own command builder (Brigadier), under `/jstech kiln`; build it as any command.

The permission is who may run it:

| Level | Constant | Like |
| --- | --- | --- |
| 0 | `SeriesCommands.EVERYONE` | `/help` |
| 2 | `SeriesCommands.GAME_MASTERS` | `/give` |
| 3 | `SeriesCommands.ADMINS` | `/ban` |

A name another mod declared is refused with an exception, while the game loads.

Each command also has a permission node, `jscore.command.<name>` (*added 2026-10-15*): a source at the command's
level always may run it, and a server's permission mod may grant it to a player below that level
([Protection](PROTECTION.md#permissions)).

## The Core's commands

*Added 2026-10-05.*

| Command | Who | What it does |
| --- | --- | --- |
| `/jstech progress <players> <axis> [<step>]` | level 2 | Shows where players are on a progression axis, or puts them on a step ([Progression](PROGRESSION.md)). |
| `/jstech chunks <players> [release]` | level 2 | Shows how many chunks players keep loaded, or lets them all go ([World](WORLD.md#keeping-chunks-loaded)). |
| `/jstech dimension create <id> <template>` | level 3 | Makes a dimension from a declared one, while the game runs ([World](WORLD.md#dimensions-made-while-the-game-runs)). |
| `/jstech dimension remove <id>` | level 3 | Takes away a dimension made that way; refuses any other. |
| `/jstech media` | level 3 | Shows how many recordings the server keeps and how much room they take. |
| `/jstech media prune <days>` | level 3 | Deletes recordings nobody used for that many days and nothing keeps ([Sound](SOUND.md#recordings)). |
| `/jstech log [<system> [<level>]]` | level 3 | Lists how much each system writes to the log, or one system's level, or sets it until the game stops ([While you build](DEVELOPING.md#the-log-one-part-at-a-time)). *Added 2026-10-15.* |
| `/jstech config get <file> <setting>` | level 2 | Says a setting's value ([Settings](SETTINGS.md#a-servers-settings-from-afar)). *Added 2026-10-15.* |
| `/jstech config set <file> <setting> <value>` | level 2 | Sets it for every player at once; the value is JSON or plain words. *Added 2026-10-15.* |
| `/jstech config reset <file> <setting>` | level 2 | Puts it back to its default. *Added 2026-10-15.* |
| `/jstech config preset <file> <name>` | level 2 | Picks one of the file's presets, shipped or saved on the server. *Added 2026-10-15.* |
| `/jstech dump permissions` | level 3 | Writes every permission of the series, and the LuckPerms groups to paste, into `jstech-permissions/` ([Protection](PROTECTION.md#permissions)). *Added 2026-10-15.* |
| `/jstech audit [page]`, `audit player <name> [page]`, `audit at <pos> [page]` | level 3 | Reads the audit log, newest first ([Protection](PROTECTION.md#the-audit-log)). *Added 2026-10-15.* |
| `/jstech health` | level 3 | The state of the series' systems and their warnings ([Diagnostics](DIAGNOSTICS.md#jstech-health)). *Added 2026-10-15.* |
| `/jstech bandwidth [<player>]` | level 3 | What the server sent over the last minute, to everyone or one player: a graph, by mod, by kind and the biggest senders ([Diagnostics](DIAGNOSTICS.md#network-use)). *Added 2026-10-15.* |
| `/jstech cost [<seconds>]` | level 3 | Measures what the series' blocks cost the server for some seconds (10 by default, 60 at most), then lists the worst places with their owners ([Diagnostics](DIAGNOSTICS.md#what-weighs)). *Added 2026-10-15.* |
| `/jstech diagnostics` | level 3 | Writes the diagnostic file for a bug report into `jstech-diagnostics/` ([Diagnostics](DIAGNOSTICS.md#the-diagnostic-file-and-crash-reports)). *Added 2026-10-15.* |

## What can go wrong

- **"Unknown command".** The player lacks the permission level, or the mod that declares it is not installed on the
  server.
- **The game will not load: the name is taken.** Another mod declared a command of that name under `/jstech`.
  Choose another.
- **A command that names players does not find one.** Player arguments take selectors (`@a`, `@s`) and the names
  of players who are online.

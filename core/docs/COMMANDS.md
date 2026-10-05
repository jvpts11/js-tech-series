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

## What can go wrong

- **"Unknown command".** The player lacks the permission level, or the mod that declares it is not installed on the
  server.
- **The game will not load: the name is taken.** Another mod declared a command of that name under `/jstech`.
  Choose another.
- **A command that names players does not find one.** Player arguments take selectors (`@a`, `@s`) and the names
  of players who are online.

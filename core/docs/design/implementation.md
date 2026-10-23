# Implementation

Performance, diagnostics, the Core's settings, and how its state is kept.

## What exists today

Everything this page designs is built, as of October 2026: the performance rules, the diagnostics, the settings and the
state. How a mod uses them, with code: [Diagnostics](../DIAGNOSTICS.md), [Settings](../SETTINGS.md) and
[State and saves](../STATE_AND_SAVES.md).

## The design

### Performance

**Nothing in the Core may weigh on the server's ticks.** Work per tick stays close to constant time, and what is heavy
runs off the tick, with its result landing on the main thread.

- **Networks and grids** are worked out again only when they change, never per cable and per tick.
- **Rooms, multiblocks and veins** are checked only when a block near them changes.
- **Fields over chunks** (pollution, fallout, radiation) spread and fade in batches, at a low rate.
- **Moving structures** run their physics away from the main thread where they can, and sleep at rest.
- **World generation** is safe across threads and always the same for the same seed.
- **Large edits and generation in old chunks** are placed within a budget per tick.
- **Machines tick only when they have something to do.**

### Diagnostics

Profiling is Spark's, which does it very well: the Core doesn't make a profiler of its own, it **names its sections in
Spark's profiles**, through a guarded adapter, so Spark shows the Core from inside. The timings on the F3 screen stay.
Besides that, the Core does only what Spark doesn't:

- **Finding what weighs in the world**: a heat map of the cost of each machine, block entity and structure, seen in the
  world as Observable shows it, and a command that lists the worst, with each one's owner.
- **Network bandwidth by system**: what each system sends to each player, with a graph and the biggest senders.
- **A watchdog for stalls**: a section of the Core that takes too long writes its stack and its system's name to the
  log, once, and warns the server's operators.
- **Slowing down gracefully**: a system over its budget slows itself (fields spreading more slowly, for one) instead of
  weighing on the tick, and says so.
- **`/jstech health`**: the state of the systems (how many networks, rooms and structures, chunks loaded per owner,
  large edits waiting) and its warnings.
- **Richer crash reports**: a section of the Core with the state of its systems and what was happening.
- **A diagnostic bundle**: a command writes the settings, the list of mods, the timings and the state to a file, for a
  player to attach to a bug report.

### Settings

The Core's own settings are in its files; J's Computers' numbers are in J's Computers' file,
`jscomputers-server.toml`, and the Operations framework's in the series' own, `jstech-operations.toml`
([The platform](platform.md)). They are settings of consequence: none turns a part of a mod off.

| Setting | Default | Page |
| --- | --- | --- |
| Overvoltage damages machines | on | [Energy](energy.md) |
| Accept any voltage | off | [Energy](energy.md) |
| Accept FE directly | off | [Energy](energy.md) |
| The rate between FE and joules | 1 FE = 1 J | [Energy](energy.md) |
| Electric shock | on | [Energy](energy.md) |
| Lightning surges | on | [Energy](energy.md) |
| Ionizing radiation | on | [Hazards](hazards.md) |
| Minerals' radioactivity | on | [Hazards](hazards.md) |
| Dissonance | on | [Hazards](hazards.md) |
| Pollution | on | [Hazards](hazards.md) |
| Hazards of items | on | [Hazards](hazards.md) |
| The pull of decompression | on | [Sealed rooms](sealed-rooms.md) |
| Fire in pure oxygen | on | [Sealed rooms](sealed-rooms.md) |
| Fire needs oxygen | on | [Sealed rooms](sealed-rooms.md) |
| Mobs breathe | on | [Sealed rooms](sealed-rooms.md) |
| Plants need air | on | [Sealed rooms](sealed-rooms.md) |
| Blocks burst over their pressure | on | [Sealed rooms](sealed-rooms.md) |
| The largest room | a number of blocks | [Sealed rooms](sealed-rooms.md) |
| The vein system | on; off, ores generate as in vanilla | [The world](world.md) |
| Each deposit type's rarity, spread and density | | [The world](world.md) |
| Don't generate a material's ore | off | [The world](world.md) |
| Try brute-force oregen | off | [The world](world.md) |
| Chunks each owner may keep loaded | 25 | [The world](world.md) |
| Chunks kept while their owner is away | on | [The world](world.md) |
| Hours chunks are kept for an owner away | 0, for as long as the owner is away | [The world](world.md) |
| The server's budget of loaded chunks | | [The world](world.md) |
| Any mod's structures turned off, spaced or kept to biomes (`jstech-structures.toml`) | as each mod has them | [The world](world.md) |
| How long an Operation waits, how it ages, how long a saved one is kept (`jstech-operations.toml`) | 1200 ticks, 600 ticks, 24 hours | [Networks and Operations](networks-and-operations.md) |
| The Space Persistor's energy | | [The world](world.md) |
| Naming discovered elements | on | [Materials](materials.md) |
| Requiring the "-ium" ending | off | [Materials](materials.md) |
| Tips at the right time (each player's) | on | [Manuals](manuals.md) |
| Less motion (each player's) | off | [The interface](interface.md) |

Empty defaults are set when the system is built. Every number on these pages is a first estimate, to be tuned in play.

### State

- **Per player**: progress on the axes, hazards built up (dose, contamination, lungs, dissonance), notes in manuals.
- **Per team**: milestones, knowledge, and what each mod's research and discovery reach.
- **Per server**: the names given to discovered elements, the carbon dioxide of the planet, loaded-chunk tickets.
- **Per dimension and chunk**: rooms and their air, fields over chunks, reservoirs, the state of each structure,
  generation done in old chunks.
- **Moving structures**: their region of blocks and where each structure is, kept through restarts.

Everything saved has a version and a way to read older saves; a save from a newer version is never written over.

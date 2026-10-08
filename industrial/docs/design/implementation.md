# Implementation

Performance, the settings and their keys, compatibility with other mods, and how state is saved.

## What exists today

J's Industrial has its three machines, the Coal Generator and one energy cable, built on J's Core's machine framework,
cable block and energy grid. It has no settings of its own yet; J's Core's settings system (files, screen, ranges) is
ready for them.

## To build

### Performance

**Nothing in the mod may weigh on the server's TPS.** Work per tick stays close to O(1), and what is heavy runs off the
tick, on a virtual thread, with its result landing on the main thread.

- **The energy grid** works out the loss of each way once, when its topology changes, not every tick.
- **Belts** keep their items as compressed queues, not as entities.
- **Pollution, radiation and fallout** are kept per chunk and spread at a low rate, not every tick.
- **The Aleph's laws** are read per region, in O(1).
- **Machines tick only when they have something to do**; a machine waiting for input costs nothing.
- **Giant multiblocks** (the Creation Forge, the LHC, the Superheavy Fusion Ring) run from their controller; their other
  blocks don't tick.
- **Reactors** simulate their physics (temperature, xenon, decay heat) at a coarse step, not every tick.

### Settings

J's Industrial's own settings live in `jsindustrial-server.toml`; the mechanics of J's Core (its energy, radiation,
pollution, dissonance, the world generation kit, element naming) keep theirs in J's Core's files. All of them show in
J's Core's settings screen, with their range, default, comment and unit. They are consequence settings: none turns a
part of the mod off as a "module".

| Setting | Default | Page |
| --- | --- | --- |
| The vein system | on; off, ores generate as in vanilla, scattered | [The world](world.md) |
| Each deposit type's rarity, spread and density | | [The world](world.md) |
| Don't generate a material's ore | off | [The world](world.md) |
| Try brute-force oregen | off | [The world](world.md) |
| Minerals' radioactivity | on | [Materials](materials.md) |
| Overvoltage damages machines | on; off, a machine just doesn't start | [Energy](energy.md) |
| Accept any voltage | off | [Energy](energy.md) |
| Accept FE directly, both ways | off | [Energy](energy.md) |
| The rate between FE and joules | 1 FE = 1 J | [Energy](energy.md) |
| Electric shock | on | [Energy](energy.md) |
| Lightning surges | on | [Energy](energy.md) |
| Pipes fail | on; off, the wrong content doesn't go in | [Fluids and gases](fluids-and-gases.md) |
| Leaks are hazardous | on; off, a leak only loses what it carried | [Fluids and gases](fluids-and-gases.md) |
| Hydrogen embrittles steel | on | [Fluids and gases](fluids-and-gases.md) |
| Cryogenic boil-off | on | [Fluids and gases](fluids-and-gases.md) |
| Boilers explode | on; off, a blocked boiler just stops | [Fluids and gases](fluids-and-gases.md) |
| Pollution | on | [Chemistry](chemistry.md) |
| `require_research` | on; off, everything is unlocked from the start | [Science](science.md) |
| The industrial chain as the only path of J's Computers' hardware | off | [Clean Room](clean-room.md) |
| The reactor explosion | on, with its thresholds (SCRAM at 150%, damage at 175%, meltdown at 200%), radius, corium and decay times | [Nuclear](nuclear.md) |
| The Quantum Link exists | on | [Exotic materials](exotic-materials.md) |
| Wormholes cost nothing | off | [Exotic materials](exotic-materials.md) |
| Easy Wormhole Gate recipe | off | [Exotic materials](exotic-materials.md) |
| The Aleph | on | [The Aleph](aleph.md) |
| Dissonance | on | [The Aleph](aleph.md) |
| Swords, and each other kind of tool | on | [Tools and armour](tools-and-armour.md) |
| Discoverers name the elements beyond 118 | on | [Materials](materials.md) |
| Require the "-ium" ending | off | [Materials](materials.md) |
| Radiation's effects | on | [Nuclear](nuclear.md) |

### Compatibility

- **Other mods' energy** reaches J's Energy through J's Core's FE converters, or directly with "accept FE directly".
- **Other mods' ores** can be moved into veins by "try brute-force oregen", through their common tags (`c:ores/...`),
  for mods that generate ores as data.
- **JEI and EMI** show every recipe, marking the ones that need research with the discovery they need.
- **Mekanism's chemicals** move through J's Core's chemical bridge.
- J's Industrial's materials carry the common tags (`c:ingots/...`, `c:plates/...`), so other mods' recipes take them.
- Optional mods are only ever reached behind a guard.

### Saving state

- Machines and multiblocks keep their state in their block entities; a formed multiblock keeps it in its controller.
- **Reservoirs** of oil, gas and brine are kept in J's Core's data over areas, so other mods read them.
- **Pollution, radiation and fallout** are kept per chunk.
- **Research** is kept per team, as a J's Core progression.
- **Element names** given by discoverers are kept with the world and synchronised to every player.
- **The Aleph's** regions of laws are kept with its dimension.

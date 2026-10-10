# Sealed rooms

A kit for rooms that hold an atmosphere of their own: a base on Mars or the Moon filled with air so the player takes off
the suit, an underwater base kept dry, the cabin of a starship. J's Core finds the rooms and keeps their air, by real
physics; the mods bring the blocks that seal them and fill them. It goes further than any one mod of the series needs,
on purpose, so any mod that wants rooms with air finds everything it needs. It is in the spirit of the oxygen of
Galacticraft and Ad Astra and the atmospheres of Advanced Rocketry.

## What exists today

Nothing of the kit. The Core's dimension rules already say whether a dimension's air can be breathed, its temperature
and its pressure, and the Core posts a hazard event before they harm a player ([The world](world.md)).

## To build

### Rooms

- **The kit finds closed volumes**, of any size, from a cupboard to a hangar of thousands of blocks. A room is worked
  out again **only when a block near it changes**, away from the game's tick, up to a size limit the server sets.
- **Rooms make a graph.** Rooms joined by doors are separate rooms: with a door shut each keeps its own air, with it
  open their atmospheres **mix over time**. A base is a set of rooms, not one volume.
- **What a player inside breathes** is the room's air; outside, it is the air of the place, from the dimension's rules,
  its bands by altitude and the physics kit.
- **A sealed room keeps its warmth**: it is always tempered, and the danger of heat and cold is for whoever goes out.

### Real gases

- **Any registered gas** can be in a room, as a mixture with the amount of each
  ([States of matter](states-of-matter.md)).
- **Pressure follows from the amount and the volume**, as an ideal gas does.
- **Breathing goes by the partial pressure of oxygen**, as in real life: below about 16 kPa of oxygen comes hypoxia,
  whatever the total pressure.
- **Gas flows** between rooms, or out, **in proportion to the difference in pressure and the size of the opening**: a
  hole of one block empties a room in seconds, a crack in minutes.

### Blocks that seal

What each block does is **data**, so any mod's blocks and any data pack take part:

- a **leak rate**: a full block doesn't leak, vanilla glass and doors leak slowly, an open block lets everything
  through, a pressurized window or door (a mod's) doesn't leak;
- a **pressure rating**: a block that faces a difference in pressure above its rating **bursts**, as a weak window does
  under water or between the clouds of Venus.

### Under water

- **A sealed room under water stays dry**: the water inside it is pumped out, and it holds air like any other room.
- **A broken window floods it**: the water rushes in through the hole, as fast as the depth's pressure pushes it.

### What the air does

- **Too little oxygen** suffocates.
- **Too much carbon dioxide** gives a headache and nausea first, then danger: the International Space Station lives at
  about 0.4%, ten times the Earth's air, and above about 5% it is dangerous.
- **Pure oxygen** is cheaper to fill a room with, at a third of an atmosphere as in Apollo and Gemini, but **fire turns
  into an inferno**: any flame, lava or spark spreads at once, as in Apollo 1. Pure oxygen at full pressure poisons,
  slowly.
- **No oxygen, no fire**: torches, candles and furnaces go out in a vacuum, and machines that burn fuel need oxygen to
  run.
- **Decompression**: when a wall breaks or the room opens to vacuum, the air rushes out in a jet, pulling items and
  players towards the hole, and the room stays empty until it is patched and filled again.

### Living things

- **Mobs breathe too**, by their kind: a cow suffocates in a vacuum, a zombie doesn't.
- **Plants need air to grow**: in a sealed vacuum nothing grows. Leaves and crops inside a room slowly turn carbon
  dioxide into oxygen, so a greenhouse helps a room breathe.

### Seeing and hearing

- **A view of the room**, through the Atmosphere Detector or a pair of goggles: the room's outline, its pressure and its
  air, and **the blocks that leak, marked**, so finding a leak is never a hunt.
- **Sound**: a leak hisses from where it is; inside a room with air, sound is heard as usual; in a vacuum it doesn't
  carry.

### Lines, structures and code

- **Vents join a room to the gas lines**: a vent on a gas pipe fills the room, or empties it.
- **Blocks can give or take gases** in a room: a mod declares its sources and sinks (J's Space's scrubbers and oxygen
  generators).
- **Rooms work inside moving structures**: a starship's cabin is a sealed room
  ([Moving structures](moving-structures.md)).
- **The API**: the room at a position, its gases, adding and taking gas, being told when a room changes, fills or
  leaks, and declaring how a block seals.

### The Atmosphere Detector

A block of the Core's shared content: it reads the atmosphere around it, inside a sealed room or out in the open, and
shows its composition (each gas and its share) and its pressure, and the view of the room.

### Settings

In the Core's files, as consequence settings: the pull of decompression, fire in pure oxygen, fire needing oxygen, mobs
breathing, plants needing air, and bursting blocks, each can be turned off; the largest room is a number.

### Who uses it

J's Space's bases on other worlds and the rooms of its starships first, with J's Space's blocks (pressurized doors and
windows, airlocks, vents, scrubbers, the Sabatier reactor); J's Oceanics' underwater bases next. Neither needs all of
it, and any mod may use the rest.

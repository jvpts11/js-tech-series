# Tiers and circuits

J's Industrial's progression runs from **T0 to T9**, and a tier is defined by the **circuit** the player can make. The
circuits follow the real history of electronics, from gears to quantum chips, and then go beyond it.

Industrial tiers are not J's Computers' hardware eras: the two are separate axes that never turn into each other. A
player can have an Advanced computer next to a T2 industry, or the other way round. J's Core keeps both.

## What exists today

J's Core has the industrial tiers (`IndustrialTier`, T0 to T9) as a type. No circuit exists yet.

## To build

### Three phases

| Phase | Tiers | What it is |
| --- | --- | --- |
| Foundation | T0 to T2 | electricity, grinding, basic metallurgy, petrochemistry; conveyors and fluids; plastics and the first silicon |
| Industrialisation | T3 to T5 | multiblocks, steel, alloys, the Clean Room, nuclear power, nanotechnology |
| Transcendence | T6 to T9 | quantum fabrication, antimatter, other dimensions, making things from nothing |

**Steel is the gate of the T3** ([Metallurgy](metallurgy.md)): there is no steel before it, and the big multiblocks are
made of it. The multiblocks before it are of brick, wood and iron.

J's Industrial is played on its own from T0 to T9. Nothing in it needs another mod of the series
([The series](series.md)).

### The circuits

| Tier | Circuit | What it is | How it is made |
| --- | --- | --- | --- |
| T0 | **Mechanical** | logic of cams and gears, with no electricity | by hand |
| T1 | **Electromechanical** | relays | by hand |
| T2 | **Vacuum tube** | thermionic valves on a printed board: a tungsten filament, glass and a Kovar seal | by hand, with the soldering iron |
| T3 | **Transistor** | discrete silicon transistors, grown in the Czochralski furnace, on a printed board | the Precision Assembler |
| T4 | **Integrated** | the first made by lithography, with the ultraviolet light of mercury lamps | the [Clean Room](clean-room.md) |
| T5 | **Nanoscale** | deep ultraviolet lithography, then extreme ultraviolet | the Clean Room |
| T6 | **Quantum** | quantum states laid layer by layer | the Clean Room beside a microgravity chamber |
| T7 | **Singularity** | built with controlled antimatter microexplosions | the Clean Room beside an antimatter chamber |
| T8 | **Transcendent** | circuits made across a wormhole | the Clean Room beside an open Wormhole Gate ([Exotic materials](exotic-materials.md)) |
| T9 | **Omniversal** | materialised directly | the Creation Forge, with no Clean Room |

What T6 to T9 need is decided with [Science](science.md) and [Exotic materials](exotic-materials.md).

**The tier a player has reached** is a progression axis of J's Core: it moves forward the first time the player makes a
circuit of that tier, and any mod can read it.

The circuits are J's Industrial's own, because they are its axis of progression. The other mods of the series use J's
Core's generic components ([Materials](materials.md)).

### Making circuits in quantity

The first three circuits can always be made by hand. Each circuit also has its machines, as in the real industry of its
time:

- **Mechanical and electromechanical:** the **Assembler** (T1), the first electric assembler of parts with several
  ingredients, faster and with more yield than the crafting table. The **relays** come from the **Coil Winder**
  ([Metallurgy](metallurgy.md)), which already winds their coils.
- **Vacuum tube** (T2), a small line, as in the factories of the 1950s:
  1. **printed boards**: a phenolic laminate (from petrochemistry) with copper foil, etched with ferric chloride in the
     **Etching Tank**;
  2. **tubes**: the glass bulb, the tungsten filament and the Kovar seal come together in the Assembler, and the
     **Vacuum Pump** (T2) takes the air out. The same pump later serves the vacuum furnace and the Clean Room;
  3. **soldering**: the **Wave Soldering Machine** (T2) solders a whole board at once, which is what made printed boards
     possible to produce in quantity.
- **Transistor** (T3): the **Precision Assembler**.
- **Integrated and beyond:** the Clean Room.

With J's Computers installed, its Crafting Computers and Crafting Interfaces automate these machines like any other.
Without it, machines take their ingredients by conveyor, pipe or hopper.

The machines' numbers are in [Machines](machines.md).

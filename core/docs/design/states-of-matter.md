# States of matter

How J's Core keeps solids, liquids, gases and the rest apart, each with its own way of being stored, carried and
behaving, instead of the usual "items and fluids", where "fluid" ends up meaning everything that is not an item.

## What exists today

Everything this page designs is built, as of October 2026, except what *Still to build* at the end lists: each state
its own, with its storage, its line and its bridges, pressure that bursts, and the containment of exotic matter with
the Antimatter Trap. How a mod uses it, with code: [Energy and resources](../ENERGY_AND_RESOURCES.md).

## The design

### Each state its own

| State | Kept in | Carried in | How it behaves |
| --- | --- | --- | --- |
| **Solid** (item) | inventories | any mod's item transport, such as J's Industrial's conveyors | as always |
| **Liquid** | tanks | pipes | it stays a NeoForge fluid, so every mod's tanks and pipes work with it |
| **Gas** | pressurised tanks, which hold their volume times their pressure | gas pipes, which hold pressure | it rises and is lost if released; a capability of J's Core |
| **Supercritical fluid** | pressure vessels | high-pressure pipes | neither liquid nor gas, as the supercritical carbon dioxide of aerogel making |
| **Slurry** | tanks | lined pipes | solids suspended in a liquid (drilling mud, dissolved ores); abrasive to unlined pipe |
| **Plasma** | magnetic containment | plasma conduits | it needs containment and energy, and dissipates without them; a capability of J's Core |
| **Exotic matter** | containment | exotic matter conduits | each form in the state it calls for, always contained (below) |

- **Every state is a kind of storage of its own**, wherever things are stored, J's Computers' network included.
- **Lines of different states never join**: each has its own line of the cable block
  ([Cables and lines](cables-and-lines.md)).
- **Bridges** bring in other mods' gases (fluids tagged as gases, Mekanism's gases), so none is left out. Liquids need
  no bridge, being NeoForge fluids already. Mekanism keeps four kinds of chemical in one registry: its gases are gases
  (rendered as a gas or not), its slurries are slurries, and its infusions and pigments are no state of matter of
  their own, so no line of the Core carries them; Mekanism's own tubes still do.
- **Changing state** is a mod's recipe (a liquefier, a boiler, a plasma torch); the Core knows the states and how each
  is kept, not how to go from one to another.

### Pressure that bursts

What is held under pressure, a gas or a supercritical fluid, can be held past what its vessel stands, and then the
vessel bursts, as a real one does.

- **A tank may say what it stands**: a pressure above the one it is filled to. Nothing fills it past its own pressure
  by itself; something stronger than it can (a compressor, a hotter gas), and once it holds more than it stands it
  bursts: its block is gone, what it held is let out, and a blast goes off with **the energy the gas held** above the
  air around it, Brode's measure, through the Core's explosions ([Explosions and fire](explosions-and-fire.md)).
- **A relief valve** opens past its setting, between the tank's own pressure and the one it bursts at, and lets a
  little out each tick until the tank is back at its setting. A valve too small for what is forced in does not save
  the tank.
- **A pipe may say the pressure it holds.** A store holding a gas at more is carried none of, and the gas reaches the
  pipe as something it is not made for, as a fluid too hot does: by the pipe's own rule it holds, or it bursts with the
  force of the gas a block of it held.
- A tank or a pipe that says nothing never bursts; each mod's design says which of its own do, and at what.
- **Valves on a line are the Core's**, for any mod's pipes: a block between two runs of a liquid or a gas line that
  lets through one way only, always (a check valve), by hand or by a redstone signal; one may let a single fluid
  through alone, and every one counts its flow for a comparator, as a flow meter. A **relief valve block** stands
  against a vessel and lets its excess out above its setting, into a discharge tank or the air, so a heated vessel
  that has one never bursts.

### Containment of exotic matter

The containers and conduits that hold neutronium, degenerate matter, antimatter and the other exotic forms are
**J's Core's blocks**, shared: J's Industrial keeps in them what it makes, and J's Space what it gathers from dead stars
and radiation belts, with no other mod installed.

- Containment **spends energy** for as long as it holds something.
- **Without energy it stops taking more and releases nothing**: there are no accidents with exotic matter.
- The **Antimatter Trap** is containment in its form for antimatter, a Penning trap.
- Each mod gives the blocks its recipes, as with the rest of the shared content.

## Still to build

- **J's Computers' storage keeping every state**, when J's Computers moves onto the Core.

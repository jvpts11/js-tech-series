# States of matter

How J's Core keeps solids, liquids, gases and the rest apart, each with its own way of being stored, carried and
behaving, instead of the usual "items and fluids", where "fluid" ends up meaning everything that is not an item.

## What exists today

Everything moves through NeoForge's standard capabilities, so it works with the machines and pipes of any mod
([Energy and resources](../ENERGY_AND_RESOURCES.md)):

- **Items**, in inventories, with faces a machine's owner sets (in, out, both, closed) named from the block's own front,
  and filters by item, id or tag with amounts.
- **Fluids**, as NeoForge fluids, in tanks and in pipes of the cable block. A pipe stands a range of temperatures and
  takes some **marks**: `jscore:gases` (a gas needs a pipe that holds pressure) and `jscore:corrosive`; they are data
  tags, so a data pack can mark any mod's fluids. A run carries only what every pipe of it takes. Amounts are in mB: a
  block is one cubic metre, a thousand mB.
- **Chemicals** that are neither items nor fluids (Mekanism's gases, for example) move through **bridges**, as an id and
  an amount, with no mod named in the Core; without a bridge there are none, and nothing breaks.
- Moving between two stores is all or nothing.

## To build

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
  no bridge, being NeoForge fluids already.
- **Changing state** is a mod's recipe (a liquefier, a boiler, a plasma torch); the Core knows the states and how each
  is kept, not how to go from one to another.

### Containment of exotic matter

The containers and conduits that hold neutronium, degenerate matter, antimatter and the other exotic forms are
**J's Core's blocks**, shared: J's Industrial keeps in them what it makes, and J's Space what it gathers from dead stars
and radiation belts, with no other mod installed.

- Containment **spends energy** for as long as it holds something.
- **Without energy it stops taking more and releases nothing**: there are no accidents with exotic matter.
- The **Antimatter Trap** is containment in its form for antimatter, a Penning trap.
- Each mod gives the blocks its recipes, as with the rest of the shared content.

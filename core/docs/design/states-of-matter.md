# States of matter

How J's Core keeps solids, liquids, gases and the rest apart, each with its own way of being stored, carried and
behaving, instead of the usual "items and fluids", where "fluid" ends up meaning everything that is not an item.

## What exists today

Everything this page designs is built, as of October 2026, except what *Still to build* at the end lists: each state
its own, with its storage, its line and its bridges, and the containment of exotic matter with the Antimatter Trap. How
a mod uses it, with code: [Energy and resources](../ENERGY_AND_RESOURCES.md).

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

## Still to build

- **Pressure that bursts**: a tank or a pipe held past what it stands bursts, with the force of what it held; a relief
  valve lets a little out before that.
- **The bridge of chemicals checked against Mekanism's four kinds** of its current version.
- **J's Computers' storage keeping every state**, when J's Computers moves onto the Core.

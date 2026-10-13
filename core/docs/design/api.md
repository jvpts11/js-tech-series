# The API

What J's Core promises to a mod built on it: which parts it may hold on to and expect to keep working from one release
to the next, how that promise grows, and how it is checked. The rules for the series as a whole are in the series' API
page ([The API of the J's Tech Series](../../../docs/API.md)).

## What exists today

- **The promise is narrow**: the packages `dev.jstech.core.api` and `dev.jstech.core.api.client`, and the few types a
  mod can't add anything without (the registries of programming languages and of kinds of Operation, and what goes into
  them). Everything else in the Core works, is documented, and is used by the series every day, but is not promised: a
  release may change its shape, and the changelog says so.
- **One version number** for the Core's API (4 today), raised by one whenever something is added to it or changes shape,
  so a mod can refuse to load below the number that brought what it needs. What a release adds is marked
  **experimental** for that release and loses the mark when the next cycle begins; the CHANGELOG has an "API" section.
- **Not settled yet**, and the API says so: until J's Computers and J's Industrial are finished, anything in it may
  change shape between releases, always with a note in the changelog.
- **The promise is written down line by line**: every type and member a mod can reach, with the version that brought it,
  and the tests fail when the code and that list disagree.
- **Taking something away**: it is marked deprecated first, with what to use instead, stays one whole cycle, and goes in
  the next one.
- **Numbers that travel** (the kinds of Operation, an Operation's status, sizes and tiers of hardware), written into
  saves and sent as numbers, never change once given out, and a test fails if one moves.
- **How a mod adds things**: it listens for the Core's register event while the game loads and adds what it has. After
  loading every registry closes and refuses changes, so what a world knows how to do never changes under it. Two entries
  with one id are refused; nothing quietly replaces anything, because which of two won would depend on the order the
  mods loaded in.

## To build

### Two layers, kept apart

The Core is one jar with two layers ([Overview](overview.md)): **the platform**, generic, for any technology mod, and
**the series' model** (eras, tiers, materials, the network's roles, kinds of Operation), which exists because the mods
of the series must agree on one identity of each. Three questions sort a part: would a technology mod outside the series
use it without knowing J's Computers or J's Industrial (the platform); does it exist because the series' mods must agree
on it (the series' model); is it a concrete implementation of one mod (not the Core's at all).

**What is the series' own lives in one package, `dev.jstech.core.jstechseries`**, and everything outside it is the
platform. A mod outside the series can tell at a glance what it may build on without taking the series' model with it.
The parts are moved into their place domain by domain, with no big rewrite:

| In `jstechseries` (the series' own) | Outside it (the platform) |
| --- | --- |
| the hardware eras and the industrial tiers | progression axes, milestones, knowledge and the knowledge gate |
| the data network's model: the Mainframe, the categories, the members, topology elements, data lines by era | the cable block, its lines and grids, overhead lines, lines that group, redstone and signals, conveyors |
| the Operations framework and its kinds | state and saves, settings, teams and owners, scheduling, the event bus |
| peripheral links between a computer and its devices; J's Energy, the series' energy, built from the kit | the energy kit: any energy, or FE, made easy (units, classes of line, ports, loss, protection, conversion); states of matter and containment |
| the series' catalogue: the elements and isotopes as data, the elements beyond 118, the exotic forms, the shared fluids and components | the registries of materials, forms, properties, mineral families and host rocks; naming what is discovered |
| the shared content: the basic lines, battery and wrench, the saw, the Configuration Card, the FE converters, the Space Persistor, the Atmosphere Detector, the containment blocks, the Structure Projector, the "J's Tech" creative tab | machines, mechanical power, heat, multiblocks, multipart, the world kits, hazards, explosions and fire, sealed rooms, moving structures, entities and vehicles, tools and items |
| the `/jstech` command root, the series' look (its theme), the Technical Reference | screens, themes, fonts, motion, overlays, sound, models, lighting and effects, the guide framework, low level, testing, the tools for those who make mods and for modpacks |

What is plainly another mod's leaves the Core: the **programming languages**, which belong with programs, move to J's
Computers' API.

### A wider promise

- **What is promised** becomes the `api` packages **and a declared list of packages** beside them: the Core's kits enter
  it one by one (settings, state and save layouts, teams and owners, block entity fields, the content builders, sound,
  the GUI toolkit, multiblocks, energy, capabilities, materials), each with documentation for a reader who knows nothing
  of it.
- **Three levels of maturity**: **internal** (not promised), **experimental** (promised, may still change with a note),
  **stable**. A part is stable only after two real mods use it, it has real tests, its documentation has an example that
  compiles, its saves and what it sends between server and players are defined, and it has gone through a cycle with no
  change to its structure.
- **The test follows everything reachable.** From the promised packages and types, every type of the series reachable
  through public and protected signatures, followed to the end, must be promised too, or the build fails. A promise
  can't hand a mod a type that is not promised.

### An example mod

An official **example mod**, built only on the public API, with a build rule forbidding anything internal or anything of
J's Computers or J's Industrial: a generator, a battery, a crusher, a tank and a fluid, an energy cable and a fluid
pipe, a sensor on a cable, settings, a recipe from a data pack, state kept per server and per player, and a simple
screen. It is a tutorial, a test of the whole API, proof that the Core is general, and an alarm for anything internal
that leaks into the promise, all at once.

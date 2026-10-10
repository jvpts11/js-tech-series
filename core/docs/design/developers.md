# For those who make mods

What makes building a mod on J's Core quick and safe: capabilities, calls between a screen and the server, work away
from the tick, codecs, errors that teach, checks at build time, inspectors, logs, live reload, a Gradle plugin, recipe
viewers, optional dependencies and testing. The Core is for NeoForge and Java only.

## What exists today

- **Declaring everything once**, with the files the game reads written by the data generation
  ([Getting started](../GETTING_STARTED.md)).
- **Optional mods only behind guards**: the Core's own integrations (JEI, EMI, Jade, FTB Teams, Curios, Accessories,
  Mekanism's chemicals) load only when the mod is installed, and a test fails if one is reached from outside its guard.
- **JEI and EMI pages** for every kind of processing recipe, with no code.
- **Timings** of named sections on the F3 screen, and panels of a mod's own lines there.
- **A testing kit** for unit tests and GameTests ([Testing](../TESTING.md)).

## To build

- **Capabilities made easy**: a capability of a mod's own (for blocks, items or entities, per face) declared in one
  line; **lookups kept in a cache** that is told when they go stale, so nothing searches for a capability every tick;
  the capabilities of multiblock and multipart parts passed on to their owner by themselves.
- **Calls from a screen to the server**: a screen calls a method on the server with typed arguments, with no payload
  written by hand.
- **Work away from the tick**: a kit of tasks on virtual threads that **hands its result back to the main thread**
  safely, can be cancelled, and keeps to a budget.
- **Codecs without pain**: builders of codecs for records, the network codec taken from the save codec, and conversion
  between NBT, JSON, YAML and TOML.
- **Errors that teach**: an error while loading says which mod, which declaration and **how to fix it**, the way the
  programmers' pages end with what can go wrong, never a bare stack of exceptions.
- **Checks in the data generation**: missing textures, translations, models and loot are reported by the build, not
  found in the game.
- **Inspectors in the game**, in development: a block entity's fields live; the payloads sent each tick, with their
  sizes; the state of networks, grids, rooms and structures.
- **Logs per system**, with a level that can be changed while the game runs.
- **Live reload** of data and assets in development, besides the screens and models.
- **A Gradle plugin of the Core's**: a mod on the Core applies it and gets the dependency, the run configurations, the
  data generation, the check of the API and the licence headers.
- **Recipe viewers for every kind of recipe**, not only the machines', in one plugin for JEI, EMI and REI.
- **The kit of guarded adapters** the series uses for optional mods, open to any mod, with the test that keeps an
  optional dependency from becoming a required one by accident.

### Testing

The series' own tests are the most complete kit of their kind; the Core opens them to every mod and completes them.

- **A testing artifact** an add-on depends on in its tests only, with helpers for every kit of the Core: build a
  network, send an Operation and wait for its status; check what energy flows; run a recipe to its end; build and form a
  multiblock; seal and fill a room; assemble and move a structure; dose a player and check the band; open a menu and
  click as a player.
- **Round trips through the save**: save and reload in the middle of a test, so a block entity is shown to survive it.
- **Control of time**: many ticks run fast, and chance with a fixed seed, so results are always the same.
- **Assertions that explain**: what was expected, what was found, and at which block.
- **The client test kit opened**: open screens, click, type, and **compare with reference pictures**; it runs with no
  screen (a virtual display) and in **shards balanced by their durations**.
- **Old saves**: sample worlds of earlier versions are kept and shown to load.
- **Optional mods with and without**: an adapter is tested with the mod present and with its absence simulated.
- **The API surface test** offered to add-ons, so each keeps its own list as the series does.
- **Data generation up to date**: a test checks that the generated files match those in the repository.
- **Tests of cost**: "this machine costs less than so many microseconds a tick", with a limit that fails the build.
- **A stall monitor and a report of time** for long batteries.
- **Arenas made in code**: empty templates of several sizes, with no structures drawn by hand.

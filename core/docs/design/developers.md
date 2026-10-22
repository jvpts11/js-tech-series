# For those who make mods

What makes building a mod on J's Core quick and safe: capabilities, calls between a screen and the server, work away
from the tick, codecs, errors that teach, checks at build time, inspectors, logs, live reload, the studio and its MCP
server for agents, a Gradle plugin, recipe viewers, optional dependencies and testing. The Core is for NeoForge and
Java only.

## What exists today

Everything this page designs is built, as of October 2026: the toolkit, errors that teach, the checks of the data
generation, the log by system, live reload, the inspectors, developer mode and the studio with its tools, the MCP server
and the tools an agent plays with, the Gradle plugin and the test kit. How a mod uses it, with code:
[While you build](../DEVELOPING.md), [The toolkit](../TOOLKIT.md), [Testing](../TESTING.md) and
[The Gradle plugin](../GRADLE_PLUGIN.md). What is not built yet is under *Still to build* at the end.

## The design

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
- **Inspectors in the game**, in the studio (below): a block entity's fields live; the payloads sent each tick, with
  their sizes; the state of networks, grids, rooms and structures.
- **Logs per system**, with a level that can be changed while the game runs.
- **Live reload** of data and assets in development, besides the screens and models.
- **A Gradle plugin of the Core's**: a mod on the Core applies it and gets the dependency, the run configurations, the
  data generation, the check of the API and the licence headers.
- **Recipe viewers for every kind of recipe**, not only the machines', in one plugin for JEI, EMI and REI.
- **The kit of guarded adapters** the series uses for optional mods, open to any mod, with the test that keeps an
  optional dependency from becoming a required one by accident.

### Developer mode

- **A key in the Core, "Developer mode"**, off by default: the studio and every tool for those who make mods and
  modpacks open only with it on. On a server, the server's own key has to be on too, and only operators use the tools
  that read or change the world.

### The studio

A place in full screen for those who make mods and modpacks, in the game's own menu look. It is built around the things
of the game, never around kinds of files or a tree of scenes: a machine is opened, not a project.

- **The things of every mod on the Core**, by mod and by kind (machines, blocks, items, fluids, research), and a
  modpack's own, each with its problems; a search finds a block, an item or a screen.
- **What a thing is made of**: its model, screen, effects, sounds, names, recipe, manual page, tags, drops, settings and
  tests, each the ordinary file it always was, with its state, and each opening in its tool.
- **Checks across its parts**: what no single file can see on its own, as a screen showing two slots of a menu that has
  three, a name missing in a language, or an animation moving a bone the model does not have; each opens the file at
  its line.
- **The thing on a bench**, in a world, working, idle or jammed, to be walked around.
- **A structure built in the world made a multiblock**: caught between two corners, its pattern recorded with a letter
  for each kind of block, its ports and its layers, and written to its file, with its projector preview and its page
  in the recipe viewers, in one step ([Multiblocks](multiblocks.md)).
- **No project of its own**: the studio reads and writes the files written by hand; screens and effects are JSON in the
  game's data format ([The interface](interface.md)).
- **Java from end to end**, inside the Core's jar, with the Core's interface kit: nothing native, no browser, no other
  language.
- **Its tools**: the screen inspector, the editor on the screen itself and the new screen
  ([The interface](interface.md)); the model preview ([Models](models.md)); the effects editor in the world and the new effect
  ([Lighting and effects](lighting-and-effects.md)); and these:
  - **A block inspector**: a block entity's fields live, its capabilities on each face, and what goes in and out of it
    (energy, fluids, items) each second.
  - **The cost a tick** of each machine, in microseconds, with a heat map in the world of the dearest blocks.
  - **A payload monitor**: what goes over the network each tick, with each payload's size.
  - **A recipe bench**: write and try recipes, see conflicts (two recipes with the same inputs) and missing items, with
    the recipe viewer's preview; for a modpack, what the pack changed against the mods' own.
  - **Tags and registries**: which tags an item is in, what a tag holds, and which mod or data pack each thing came
    from.
  - **A loot tester**: roll a table a thousand times and see how the drops spread.
  - **A board of translations**: every key, what each language lacks, and the texts that do not fit their screen.
  - **The settings of every mod on the Core**: range, default and value, and a modpack's preset exported.
  - **Catching a structure**: build a multiblock in the world, select it and save it as a structure file, with the
    Structure Projector's preview ([Multiblocks](multiblocks.md)).
  - **An editor of camera paths**: fly where the camera goes and keep each place with a key, change each keyframe in
    a table, see the path from above, play it as a scene before it is saved and save it to the path's file
    ([The interface](interface.md)).
  - **Recording a test**: click through a screen and keep the path as a client test; and the tests' reference pictures
    with the difference marked.
  - **A round trip through the save**: save and reload a block and see which field changed.
  - **The logs of each system**, their level changed while the game runs, and **the errors that teach** in a panel: the
    mod, the declaration and how to fix it.

### The MCP server

The studio's tools opened to agents (Claude, GPT or any other that speaks the Model Context Protocol) and to tools
outside the game: everything the studio does, the server does, so a mod is made with an agent or without one.

- **A second key, "MCP server"**, off by default, which turns on only with developer mode on and only from inside the
  game: no file a modpack ships can turn it on.
- **Safe against hijacking**:
  - It listens only on the computer itself; nothing from the network reaches it. On a server it serves the server's own
    machine, never a player's client.
  - **Every client is paired by the player in the game**: the first time a client asks, the game shows who asks and
    what for, and the player allows it or not. Each client gets its own secret key, made new at every start, and the
    studio lists the clients and takes a key back at once.
  - A request from a web page is refused (the origin and the host of every request are checked), so a site open in a
    browser cannot reach the game.
  - **Reading and acting are apart**: reading (state, screens, pictures, logs) is one permission; acting (moving,
    clicking, placing, commands, writing files) is another, given at pairing.
  - Files are written only inside the mod's or the pack's own folders, never outside them; no shell and no code run
    from outside.
  - Game commands come from a list of safe ones (never those that give operator rights, ban, stop the server or change
    the whitelist), and only in a test world, or on a server with its developer mode on, for an operator.
  - **What players wrote** (signs, books, chat, item names) goes back marked as written by players, never as
    instructions; the tools' descriptions are the Core's own, and tools another mod adds stay off until the player
    turns that mod's on.
  - While an agent is connected a mark is on the screen, and while it acts, its name. Any key the player presses takes
    the controls back, and one key cuts every agent off at once.
  - Every call is written in a log the studio shows: who, what and when.
- **Tools to play the game, for automated tests**:
  - **Seeing**: where the player is and looks, what the crosshair points at, the inventory, the screen open and its
    tree of components (the inspector's, by stable names), a picture of the window, the blocks and block entities
    around, the chat and the log.
  - **Moving and acting as a player**: walk to a place, jump, sneak, look at a block or an entity, pick a hotbar slot,
    use, break, place, open a block, click a component by its name, type, press a key, close the screen. It goes
    through the same input the player uses, so the test is real.
  - **The test world**: give items, set blocks, the time and the weather, run the allowed commands, save and reload,
    and run the GameTests and the client tests.
  - **Waiting**: for ticks, or until something is true (a screen opens, a block changes, a component shows a text), with
    a time limit.
  - **Recording**: what an agent does can be kept as a client test, as the studio's "Record a test" does.

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

## Still to build

- **More of the studio**: a preview of who may do what at a place; and, on the Recipes tab, a preview of what
  unification would change.
- **More for agents**: a published page of the MCP server's tools and their schemas, made from the tools themselves, and
  a tool that describes the scene in view.

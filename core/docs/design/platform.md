# The platform

The generic part of J's Core: what any technology mod needs to exist in the game, keep its state, be configured and
be told apart from the others. The systems built on it (energy, cables, machines, networks, the world, screens, sound)
have pages of their own.

## What exists today

Everything this page designs is built, as of October 2026: content declared once, text and colour, state and saves,
settings and their screen, commands, teams and owners, time and diagnostics, the Core's own settings, permissions (by
dimension and claim too) and protection. How a mod uses it, with code:
[Content](../CONTENT.md), [Text and colour](../TEXT_AND_COLOUR.md), [State and saves](../STATE_AND_SAVES.md),
[Settings](../SETTINGS.md), [Commands](../COMMANDS.md), [Ownership](../OWNERSHIP.md),
[Protection](../PROTECTION.md) and [Diagnostics](../DIAGNOSTICS.md).

## The design

### The Core's own settings

- **The Core's file keeps only the Core's settings.** `jstech-balance.toml` holds the Core's own. J's Computers'
  numbers (the disks' latency, the Subframe's share, the time programs may spend) are in J's Computers' file,
  `jscomputers-server.toml`, and the Operations framework's (how long an Operation waits, how it ages, how long a saved
  one nobody resumed is kept) in the series' own, `jstech-operations.toml`.
- **Settings of consequence** for every system of the Core that can hurt a world (radiation, dissonance, pollution,
  overvoltage) live in the Core's file, each with its page ([Implementation](implementation.md)).

### Settings for every mod

The settings kit gathers what YACL, Cloth Config and Config Menus for Forge do, in one place, for any mod.

- **The right control for each kind**: sliders for ranges, drop-down lists for words, a colour picker for colours,
  **editors for lists and maps** that add and take away entries, and **a picker of items, blocks or fluids** from a list
  with search.
- **Rich descriptions**, with pictures (animated ones too) showing what a setting does, and links that can be clicked.
- **Groups that fold away** inside a section.
- **Server settings edited from afar**: an operator changes a dedicated server's settings from their own game; the
  change goes to the server and from it to every player, with the permission checked. **Players see the server's
  values**, read-only. Operators also have `/jstech config get` and `/jstech config set`.
- **Each setting says when it takes effect**: at once, when the world reloads, or on restart; the screen says "needs a
  restart".
- **Settings that depend on others**: one shows, or is active, only when another is on; and checks across settings, with
  a message.
- **Presets**: named sets per file ("gentle consequences", "full realism"), which mods ship and a server owner picks;
  **export and import** of settings as text, to share them; **defaults for modpacks**, a folder of server settings every
  new world starts with, in every format.
- **On the screen**: undo and redo, and a filter that shows only what was changed; every control reached by keyboard
  and game controller ([The interface](interface.md)).
- **For code**: a mod is told when a setting changes, to react at once.

### Permissions and protection

The kit gathers what PermNodes, Open Parties and Claims and FTB Chunks do with a library, in one place.

- **Permission nodes for everything the Core does**: commands, opening another's machine, editing settings, using or
  breaking what has an owner, building multiblocks. They go through NeoForge's permission API, so LuckPerms and FTB
  Ranks work; without them, the operator levels apply.
- **Asked by place**: every permission is asked with the place's contexts, `jscore:dimension` and `jscore:claim` (the
  claim's owner, or none outside a claim), so a server grants a node in one dimension only, or only inside a claim,
  and asks for an owner who is away by their id.
- **The nodes written out**: LuckPerms doesn't find NeoForge mods' nodes by itself, so `/jstech dump permissions`
  writes the list and a **template group** ready to import.
- **Everything the Core moves or changes respects claimed land**: robots, machines that break and place blocks, tools
  working on the move, moving structures (a ship can't fly or crash into another's land to break it), the explosion
  kit's explosions, fire, fluids and projectiles.
- **One question, "may this actor do this here?"**, which every mod asks the Core, with adapters for FTB Chunks, Open
  Parties and Claims, and the game's spawn protection.
- **Machines act for their owner**: when a machine acts as a player, claims and permissions weigh **its owner**, never a
  generic fake player that passes everything or is stopped by everything.
- **A list of trusted players per thing**, beyond the team, with levels of access: use, set up, break.
- **An audit log**, optional, of who used, changed or broke what among owned things, with a command to read it, for
  server administrators.
- **Limits per player** on robots, moving structures and large edits, besides loaded chunks, so nobody can bring a
  server down on purpose.
- **PvP and teams respected**: damage between players follows the server's PvP rule and the teams' friend or foe.

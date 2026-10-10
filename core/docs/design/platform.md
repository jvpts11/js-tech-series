# The platform

The generic part of J's Core: what any technology mod needs to exist in the game, keep its state, be configured and
be told apart from the others. The systems built on it (energy, cables, machines, networks, the world, screens, sound)
have pages of their own.

## What exists today

### Content

A mod declares each block and item **once**: its name, its look, what it drops, the section of its creative tab, its
tags. The data generation writes the block state, the models, the English, the loot and the tags from that one
declaration, so none of them is a list kept by hand ([Content](../CONTENT.md),
[Getting started](../GETTING_STARTED.md)).

- **Block entities** declare their state as fields, each saved, sent to the players who see the block, or shown to its
  menu, as it says: inventories, energy and tanks that pipes and cables reach and that spill when the block breaks,
  values of any kind a codec writes, parts that write themselves, block state properties that follow a value.
- **Devices** are blocks that run their block entity and open its menu; **menus** say their slots, shift-clicks,
  buttons and validity once, and place their slots from the screen's own layout, so a slot and its frame never part.
- **Items that hold things** keep their contents as data components.
- **Entities** are declared the same way, with kits for **vehicles** (a driver's input, a vehicle's specification),
  **robots** (tasks of registered types), **projectiles**, and **what is worn** (armour and modules a player wears,
  from any source) ([Entities](../ENTITIES.md)).
- **Ids that are saved** are stable: every enum value or entry saved or sent has a stable id and name, so renaming a
  constant in the code never breaks a world.

### Text and colour

Every sentence a player reads is a **key declared beside the code that says it**, with its English; the language file
is made from those declarations, and text travels between server and client as keys with their arguments, so each
player reads it in their own language. **Palettes** are the colours a screen paints with, declared once as records of
named roles and written as data, so a resource pack can recolour every mod's screens role by role
([Text and colour](../TEXT_AND_COLOUR.md)).

### State and saves

- **Values kept with the world**, declared once with their type, their default and whom they belong to: the whole
  server, a dimension, a player (online or not) or a team. Each is saved in its own file, and a synced one reaches only
  the players it belongs to, once per tick however often it changed.
- **Versions of what is saved.** Every shape a mod saves (a value, a block entity's fields, a file of its own) has a
  layout with a version and the steps that read an older one. A save written by a **newer** version of the mod is read
  as far as it can be and never written over: a copy is kept beside it, so going back to the newer mod loses nothing.
- **Values from data packs**: a table a pack maker should be able to change (fuels, rewards) is a data registry, read
  again whenever data packs reload.
- **Several moves, all or none**: a batch tries every step (items, fluids, energy) first and only then does them,
  undoing what it did if one comes short.
- **Large data** between server and players goes in pieces, compressed, at a set rate.

([State and saves](../STATE_AND_SAVES.md).)

### Settings

Each setting is declared once, with its range, its explanation, its name on the screen and its unit. A file is
**client** (one player's taste), **common** (the same everywhere) or **server** (one world's rules), in TOML, JSON,
JSON5 or YAML. A value out of its range is **pulled back** into it, with a line in the log; a value that can't be read
becomes the default; a file from a newer version is never written over. Every mod gets a **settings screen** in the
Core's look, reached from the game's list of mods, with search, sections, a card per setting and its default
([Settings](../SETTINGS.md)).

### Commands

Every mod of the series puts its commands under **one root, `/jstech`**, each with its permission level; a name two
mods want is refused while the game loads ([Commands](../COMMANDS.md)).

### Teams and owners

One answer for every mod to **which team a player is on** (FTB Teams when installed, the game's own teams otherwise, a
team of one for the teamless) and **who may use a thing** (private, team or public, with operators passing every lock),
so a machine of one mod and a robot of another agree ([Ownership](../OWNERSHIP.md)).

### Time and diagnostics

- **Sharing a tick**: a block that runs many things each tick shares its time fairly among them and stops when the
  tick's budget is spent.
- **The calendar** reads the world's time as hours, weekdays, weeks, seasons and years.
- **Timings** of named sections, with the slowest shown on the F3 screen, and panels of a mod's own lines there.
- **The Core's event bus** carries what happens inside its systems (the life of every Operation, the network's events),
  so one mod can react to another without knowing it.
- **Keys** a mod declares are sent to the server as actions, held or pressed.

## To build

### The Core's own settings

- **The Core's file keeps only the Core's settings.** Today `jstech-balance.toml` also holds J's Computers' numbers
  (the disks' latency, the Subframe's share, the time programs may spend); they move to J's Computers' own file. The
  Operations framework's settings (how long an Operation waits, how it ages) stay in the Core.
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

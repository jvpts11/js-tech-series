# Machines

The framework every machine of the series is built on, so that a machine of J's Industrial, J's Space or an add-on
behaves the same way, shows the same screen and plugs into the same lines. What each machine does, and its numbers, are
its mod's.

## What exists today

A **processing machine** for any mod ([Machines](../MACHINES.md)):

- input and output slots, fluid tanks, an energy store and upgrade slots, laid out by the machine;
- **recipe kinds**: each kind of machine works a kind of recipe, kept as data files that players and data packs add to,
  change or remove with no code; one line declares the kind, its recipe type and its name;
- each tick: with nothing to work, or no room for **everything** a recipe makes, its progress goes back to zero, so a
  machine never ends a recipe it can't finish; with no energy it waits and keeps its progress; otherwise it spends the
  tick's energy and moves on;
- **upgrades**: an item says what one of it does to speed and to energy, and the effects of every upgrade installed
  multiply;
- **JEI and EMI** pages for every recipe kind with no code, which only load when the viewer is installed;
- faces an owner sets per machine (in, out, both, closed), and what tools such as Jade show ("Working: 45%").

## To build

The framework gathers what the machines of Mekanism, Thermal and EnderIO do, in one place, for any mod.

### What every machine has

- **Resources**: items, liquids, gases, plasmas, supercritical fluids, slurries, exotic matter
  ([States of matter](states-of-matter.md)) and **heat**, and energy through a port with a voltage class
  ([Energy](energy.md)).
- **Sides per resource**: each face takes, gives, does both or nothing, for each resource. **Auto-output** pushes what
  the machine makes into the inventory or line next to it, as the discharge pumps of real machines do, and
  **auto-input** pulls from a neighbour; each with its rate and its filters, per face and per resource.
- **Its state is always in view**: the screen says whether it is working, waiting for input, has its output full, has no
  energy, is on undervoltage, has a blown fuse, is damaged, or is turned off by redstone. In the world, a working
  machine shows it, with its sound and particles for starting, working and stopping.
- **No wear.** A machine is only damaged by overvoltage, an explosion, sabotage or overheating.
- **Damage and repair.** A damaged machine looks cracked and works worse until it is repaired, the way Factorio's repair
  packs work: a **repair kit** is held and used on the machine and repairs it over a few seconds, spending itself as it
  goes. How much worse a damaged machine works, and the kits, are each mod's.
- **Heat.** A machine that makes heat has a heat port; if the heat is not taken away, it overheats and is damaged.
- **One screen for all**, from the Core's screen toolkit, with the same tabs on every machine of every mod: **Main**,
  **Sides**, **Redstone**, **Upgrades** and **Protection** (the fuse and the battery, [Energy](energy.md)). The screen
  also sets **who may use it** (private, team or public, [The platform](platform.md#teams-and-owners)) and shows
  **statistics**: items a minute, energy a second and efficiency, with a graph over time.

### Never jammed

- **A slot can be locked to an item**, shown as a ghost, so automation never fills it with the wrong thing.
- **The player picks the recipe** when several fit the same inputs.
- **Factories with several lanes**: a machine with lanes working in parallel **sorts its inputs among them** by itself,
  as Mekanism's factories do.

### Recipes

- **The recipe format covers everything**: every state of matter in and out, several outputs, byproducts by chance,
  catalysts that are not used up, conditions of temperature and pressure, and the pieces of knowledge a recipe asks for
  ([Progression](progression.md#the-knowledge-gate)). JEI, EMI and the manuals always show a locked recipe, with its
  lock.
- **JEI and EMI fill the machine**: their "+" button moves what a recipe takes from the player's inventory into the
  machine's slots.

### Control

- **Redstone**: a machine runs always, with a signal, without one, ignores redstone, or runs **one recipe per pulse**.
- **The comparator reads what the player picks**: progress, how full the input or the output is, or the energy.
- **Declared parameters**: a machine says which of its values can be read and set from outside. That is how J's
  Industrial's Industrial Controller Computer and J's Computers' network command it, and what the network's tables
  show ([Networks and Operations](networks-and-operations.md)).

### Setting up fast

- **The Configuration Card**, a tool of the Core's shared content: it copies sides, redstone, filters and locks from one
  machine to others of the same kind, or to compatible ones with what fits, and can **paste over an area** at once, as
  Mekanism's Configuration Card and Thermal's Redprint do.
- **Sides set in the world**: looking at a machine with the wrench shows each face's role in colour, changed right
  there with no screen, as EnderIO's wrench does.
- **A wrench** takes a machine away whole: its contents, upgrades, settings and the progress of the recipe under way go
  with it; a pickaxe breaks it and drops what it held. The Core has a **basic wrench**, with a recipe of vanilla
  materials, so a mod alone has one; any other mod's wrench in the common tag `c:tools/wrench` works too.

### Growing in place

- **A machine becomes the next one without being broken**: a mod with levels of one machine upgrades it in the world,
  with its contents and settings, as Mekanism's tier installers do.
- **Families**: the variants of one machine (by era or by tier) are declared as one family with one definition.

### Upgrades

The framework gives upgrades a set of **effects** a mod's upgrade item declares: speed, energy, recipes at once, yield,
byproducts, what a recipe spends of a resource, heat, sound, buffer size. A machine has **few upgrade slots**, and an
upgrade may say how many of it fit in one machine, so a machine is fast or thrifty, hardly both. Which upgrades exist,
their numbers and the slots per machine are each mod's, in its own design.

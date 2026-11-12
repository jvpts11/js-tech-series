# The API

The public API lets other mods add to J's Computers without touching its code. Its full explanation, from zero and with
code, is in the public API guides ([J's Computers](../API.md) and the series' one, `docs/API.md`); this page keeps the
design behind it.

## What it is made of

Packages and nothing else: `dev.jstech.computers.api` (with `.client` for what is drawn in the player's game and
`.planner` for the engines' planners), plus the types an add-on has to hold to add something (`IsaSpec`, `KernelDef`,
`OsDef`, `ProgramSpec`, `DesktopEnvironmentDef`, `OperatingSpaceDef`, `EngineDef`). Everything else is internal and
changes without warning.

An add-on is an ordinary NeoForge mod that declares its dependency on J's Core and J's Computers in its
`neoforge.mods.toml`.

## How things are added

Each mod opens its registries once, during loading, by firing an event on the mod bus:

- **`CoreRegisterEvent`** (J's Core): programming languages and Operation types.
- **`ComputersRegisterEvent`** (J's Computers): instruction sets, kernels, operating systems, programs, desktops,
  operating spaces, network engines, component types for programs' windows, and what the engines that take extensions
  (NextgreIQL) add to their planner: rules, hints, statistics and notes under a plan's steps.

After loading, every registry closes and refuses changes: what a world knows how to do doesn't change under it. A
repeated id is refused (the one exception is a language, which replaces the one before with the same id, with a line in
the log). J's Computers adds its own systems, desktops, programs and Σ# through the same events, so the add-ons' path is
the one that always runs.

**In the player's game** (registered when the client starts):

- a **component type** for Σ# programs' windows (`GenericComponent`): the add-on says what it takes and registers what
  draws it, in the system's skin;
- a **program with its own window** written in Java (`DesktopApps`): it runs on the client of whoever opened it; other
  players and the monitor's face in the world see a placeholder, unless the program says what it draws is safe to show;
- a **surface** (`SurfaceRenderer`): an array of colours or a render target of the size asked for, which spends the
  machine's video memory.

A power supply can be declared self-sizing (it never refuses, and its rating is only a label); none of the mod's own is.

## What is not open, and why

- **A Σ# program's calls are fixed** ([Σ and Σ#](sigma.md)): an add-on brings a language of its own, an instruction set
  or a component type instead.
- **Sockets need no registry:** an id `namespace:path` is enough.
- **The hardware formulas** (the processor's 40, the 256 items per GB) belong to the mod.
- **The behaviour of existing Operations doesn't change;** an add-on adds new types.

## Versions and stability

- `JsCoreApi.VERSION` and `JsComputersApi.VERSION` are integers that go up with everything added to or changed in that
  mod's API; an add-on can refuse to load below the number it needs.
- `JsCoreApi.SETTLED` says whether the API's shape has settled. It hasn't: it settles when J's Computers and J's
  Industrial are finished.
- What is new in the current cycle or the last release carries `@ApiStatus.Experimental`; the mark comes off when the
  next cycle starts.
- Each mod's API is written down line by line in a file (`api/core.txt`, `api/computers.txt`), with the version that
  brought each line; a test compares the code with the file on every build. Nothing enters or leaves the API without
  someone reading it and deciding.
- Every release opens its section of the CHANGELOG with an API part.
- What is going away is marked `@Deprecated` first, with its replacement, stays a whole cycle and leaves in the next.

## What Carbon opened

**The programming languages are home.** They belong with programs, so they are J's Computers' API, not J's Core's:
`ComputersRegisterEvent` takes the languages, and `CoreRegisterEvent` keeps the kinds of Operation, which every mod of
the series shares.

**Anyone extends J's Computers:** through the API, an add-on also declares **new eras, new hardware and new computers**,
besides the programs and the rest. J's Computers registers its own hardware through them, Quince's whole family, the
cooling parts and the items of the survival chain among it, as it does its programs, so the add-ons' path is the one
that always runs. Like the other registries, they are filled by the
register events while the game loads and close after it; KubeJS's startup scripts can fill them too.

| Registry | What an entry declares |
| --- | --- |
| Eras (J's Core) | an id (`namespace:path`), a name, a colour, a **position in the order**, its years (for the installers' banners) and its installation medium |
| Processors | the era, the socket, cores, threads, gigahertz, the design's efficiency, the instruction set, the watts, graphics on the die, the name and the codename |
| Graphics cards | the era, cores, megahertz, the design's efficiency, video memory, the slot's generation, the watts, the video outputs |
| Memory | the generation, the capacity, the speed |
| Disks | the kind (hard disk, SSD, NVMe), the capacity, the era |
| Motherboards | the era, the form factor, the socket and how many, the memory generations and slots, the groups of expansion slots, the disk slots, the ports |
| Power supplies | the rating, the efficiency, the line (its base life) |
| Expansion cards | the kind, the slot |
| Cooling | the kind (cooler, tower, water, fan), the watts it takes away, the slots it takes ([Temperature](temperature.md)) |
| Computers | the case, the form factors it takes, its machine (the host scope, [Programs](programs.md)), its block, how many of each part the case counts, the watts the case takes away and its fan slots |

Every item comes with its entry, with the era and the usual fitting rules. Eras are a registry, saved by their id: an
add-on's era comes after the Singularity or between two eras, and the rules that compare eras follow the order. A
value kept per era falls back to the nearest earlier era that has one. The old instruction-set declaration
(`ArchitectureSpec`, `registerArchitecture`) is gone, and every component an item saves carries the version of its
layout, an item from before reading as version 0.

**What else Carbon opened:**

- **An event when a player reaches an era** (in J's Core, where the eras are), so other mods can gate their content
  on it ([Survival](survival.md)).
- **A computer's temperature and thermal budget**, read through a capability of J's Core, which is how J's
  Industrial's chillers join later ([Temperature](temperature.md)).
- **The kinds of recipe of the survival chain's machines**, as data, one kind per machine: JEI and EMI show them and
  KubeJS changes them.

Everything Carbon added is **experimental** for its first cycle, as J's Core's three levels say, raised
`JsComputersApi.VERSION` and is written line by line into `api/computers.txt`.


Cable types already belong to J's Core. J's Space (its machines that run as computers, the Flight Control and Aerospace
Design Computers, the Satellite Cards and the rest of its hardware, and the Enigmatic hardware) and J's Industrial (the
Industrial Controller Computer and its Fieldbus Cards, the machines that run as computers, and the hardware's industrial
recipes) build on these registries.

## To build

- The **dataset and model types** for the AI ([Artificial intelligence](ai.md)), so J's Robotics can train robots
  without J's Computers knowing what a robot is, come with the AI (Nitrogen).
- Teracoin's API for J's Economy, designed with J's Economy ([Teracoin](teracoin.md)).

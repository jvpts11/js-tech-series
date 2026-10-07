# Overview

J's Computers simulates a realistic computer network inside Minecraft. Storage and processing are physically spread
across servers and computers, and how well they perform comes from the hardware the player builds into them. Unlike
Applied Energistics 2 or Refined Storage, there is no magic controller in the middle: everything rests on real hardware
with real limits, and every piece of work the network does is done by a computer.

## Many computers, each with its job

- **The Personal Computer:** the player's own desk, their programs and their way into the network.
- **The Mainframe:** the brain of the network. It keeps the index of everything stored and carries out every request.
- **Servers, in racks:** they hold the network's storage.
- **The Crafting Computer:** it does the network's crafting.
- **The supercomputer:** it lets many crafts run at once.
- **The Cluster Management Computer:** it installs systems on the machines in the racks and watches over them.

The full list is in [Computers](computers.md).

## Principles

**Everything is data.** Items, fluids and chemicals are units of digital information, stored on disks and counted the
same way: fluids and chemicals are measured in millibuckets, and 1,000 mB (a bucket) weighs one item. The room an item
takes on a disk is the machine word of the disk's era: 1 MB (16 bits, Vintage), 16 MB (32 bits, Legacy) and 256 MB (64
bits, from the Transition on). The same disks hold folders, files, programs and operating systems, measured in the same
units ([Storage devices](storage-devices.md)). In memory, an item on its way into or out of the network takes 4 MB
([Hardware](hardware.md)).

**Simple by default, complex on demand.** J's Computers can be played two ways, over the same network:

- **The simple way:** as a classic logistics mod, like Applied Energistics 2 or Refined Storage, and as friendly as it
  can be for players used to that experience. Store, request, deposit, import, export and craft through the network's
  grid (MC-NET and its Interactor, or the Network program on any desktop), without learning any system or any language.
- **The advanced way:** the real J's Computers experience: the operating systems, IQL, Σ and Σ#, the network engines,
  the computing, and everything else the mod brings.

The simple way is not a lesser version. It is the same network, and a player moves to the advanced way whenever they
want, one piece at a time.

**Emergent complexity.** The challenge is solving the bottlenecks that appear naturally as the infrastructure grows, not
memorising arbitrary rules. Every mechanic is held to the same measure: never simplistic, never more complicated than
its job asks.

**Engineering realism.** Mechanics come from real computing: hardware eras with real parts, a memory hierarchy, disk
latency, network throughput, operating systems that behave like the real ones, a network that answers like a database.

**Computers do the work.** Nothing in the network acts on its own: storing, moving and crafting are the work of
computers. A bus on a cable is a port of the network, not a machine: what passes through it are the network's Operations
moving things. The network's intelligence is software: the Mainframe runs a **network engine**, a program installed on
it, which reads the requests and plans the work ([Operations](operations.md)). Without an engine, the network still
pulls, pushes and moves items, but refuses crafts, plans and IQL.

**Hardware is capacity, software is role.** What a computer *can* do comes from its parts: how many items it handles per
tick, how many wait in its memory, how many queues it works. What it *does* comes from what it runs: a Mainframe
orchestrates because it runs an engine; a server stores, caches reads or carries messages according to the services
installed on it; a Cluster Management Computer manages racks because it runs the Cluster Manager. Two machines with the
same hardware differ by their software.

**Real systems, parody names.** Operating systems, programs and parts stand for real ones and behave like them. Names
follow one rule: a commercial brand gets a parody (Integra, Velocion, Envya, Midsoft, Frames), and a design's codename
stays the real one (Haswell, Kepler, Conroe). Every program has a software house of the mod's world that makes it
([Programs](programs.md)).

**The monitor is the screen.** A computer's block is its case: using it opens the case, to put parts in and take them
out. Everything the computer shows appears on a monitor linked to it: firmware, system, programs. The monitor's face in
the world shows what its screen shows, live, and one player uses a monitor at a time. A computer without a monitor keeps
running; nobody sees it.

**One network, no channels.** A network is everything that reaches one Mainframe through data cables, routers and
repeaters, with a single network id. There are no channels to count and no subnetworks. What limits a cable is its speed
and its range ([The network](network.md)), and a job that needs another kind of link has its own cable line.

**Everything the network does is an Operation.** A request to the network becomes an Operation, which works like a
database statement: SELECT, INSERT, MOVE, CRAFT and the others ([Operations](operations.md)). The network's language,
IQL, is written in the same terms.

**Two ways to do everything.** What is done in a window can be done in words: a bus set up in its window can be set up
in IQL or by a Σ# program, and the window shows what a program set. Whoever likes buttons never needs the prompt, and
whoever likes the prompt never needs a button.

**Words, colours and images are data.** Every word a player reads is translatable. The colours of screens and desktops
come from palettes a resource pack can replace, and wallpapers, icons and brand marks are image files. Text a machine
keeps as data (a file's name, for example) stays in English, the language of the machines.

**Light on the server.** Work is done when something changes, not every tick: a computer's figures are worked out again
when its parts change, and the slowest cable and the length of each stretch of a network when the network changes. The
world is only touched on the server's main thread; waiting (a disk seeking) is the work of virtual threads that resume
on a tick, and a long rebuild of the index runs in the background and takes over when it is ready. A place the mod
remembers is only read if its chunk is loaded: nothing in the mod loads a chunk to look at it. Programs run on a budget
of instructions and of real time per tick ([Σ and Σ#](sigma.md)), so a busy computer slows itself down, not the server.

## Who it is for

Players of heavy technical modpacks (GregTech New Horizons, Nomifactory), from the first computer, which has to be easy
to get working, to a network that covers the whole base.

## J's Computers in the series

J's Computers (id `jsc`) is the computing mod of the J's Tech Series.

**Every mod of the series depends only on J's Core, never on another mod of the series.** J's Computers needs J's Core
and no other mod of the series; the same goes for J's Industrial, for J's Space, and for every mod of the series to
come. When several of them are installed together, they work with each other through what J's Core gives them all (the
model of the network, the Operations framework, the capabilities, the event bus) and through integrations that only wake
when the other mod is present: J's Computers' network then reaches the other mods' machines and runs their Operations,
but none of them needs it to work, and it needs none of them ([The series](series.md)).

What J's Computers needs:

- **J's Core**, the series' library. From it come the eras and the industrial tiers, the shared cable block, the model
  of the network (its members, its id, its topology), the Operations framework, values read from data packs,
  translatable text, palettes, the GUI toolkit, sound, the manuals, progression and advancements.
- **GeckoLib**, for the animated models. Every required dependency will be replaced by the series' own code in time, and
  none is added.

It works with these when they are present: **JEI** or **EMI** (the recipe list beside every monitor, and recipes taken
into the Pattern Studio), **Mekanism** (chemicals stored as data, and its machines on the network) and **CC: Tweaked**
(the Network Gateway, [Peripherals](peripherals.md)). A machine from any mod, of the series or not, joins the network
through the standard item, fluid and energy capabilities, and through Mekanism's chemicals when it is present. How the
mods of the series fit together is in [The series](series.md).

## Configuration

There are no module switches: a module is on when the mod of the J's Tech Series that brings it is installed. J's
Computers does not turn parts of itself on or off; what can be adjusted is numbers and behaviour, in three files. Each
key has a range, a default, a comment and a unit; a value outside its range is pulled back into it, and a value that
cannot be read returns to the default. All three also appear on J's Core's settings screen.

| File | Where | What it holds |
| --- | --- | --- |
| `jstech-balance.toml` (J's Core, server) | the world's `serverconfig` | The Core's balance that computing uses: how long an Operation waits, how fast waiting Operations climb in priority, each disk type's wait, the Subframe's share, how long an orphaned Operation is kept, the real time programs run per tick (per machine and per server), media and the calendar. |
| `jscomputers-server.toml` (J's Computers, server) | the world's `serverconfig` | Booting (the boot menu), installing Arch and Gentoo by hand, the `listcmd` command, components that reach outside the game, and Soundfoundry's catalogue and speeds. |
| `jscomputers-client.toml` (J's Computers, each player) | the player's `config` | Reduced motion on the desktops, each system's cursors, components that reach outside the game. |

Every key, with its default and range, is listed in [Implementation](implementation.md).

## To build

The systems still to build follow the same rule:

- **Wireless** is an essential part of the mod, like the cables: it cannot be turned off ([Wireless](wireless.md)).
- Temperature, Teracoin, the AI and the simulator have no on and off switch. Each gets its number keys when it is built.
- What creates items out of nothing (Teracoin's 3D Printer and the AI's item generation) has its own list of items in
  the server's configuration, one for each, and only what is on the list can be created ([Teracoin](teracoin.md),
  [Artificial intelligence](ai.md)).

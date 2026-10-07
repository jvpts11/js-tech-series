# Autocrafting

How the network makes things on its own: patterns, the Crafting Computer, the supercomputer, the machines it drives and
how a craft is planned.

## The words

- **Pattern:** a recipe the network knows, what goes in and what comes out. There are three kinds:
  - a **bench** pattern: a 3×3 grid and its result (a cell can take any item of a tag, "any planks");
  - a **machine** pattern: what to put in a machine and what it gives back, with a time limit and, for an output that
    only comes sometimes, its probability;
  - a **multi-stage** pattern (a pipeline): stages of both kinds, in order, each starting when the one before has made
    its result.
- **Crafting Computer:** the computer that crafts. Its **Crafting Cards** decide how much it drives, and keep the bench
  patterns in their own memory.
- **Crafting Interface:** it sits on the crafting cable next to a machine, keeps that machine's patterns and feeds it.

## Two steps of growth

**Without a supercomputer** (the start: a Crafting Computer on its own), the Mainframe owns the CRAFT Operation and its
plan (the network engine, [Operations](operations.md)), and a Crafting Computer drives the craft. A Crafting Computer
claims one **whole** craft at a time, for itself. Inside that craft, the machine stages run in parallel up to its cards'
threads (1, 2, 4, 6 or 8 per card, added up). Automation works from the start of the game; the limit (one craft at a
time per computer) is the natural reason to build the first supercomputer.

**With a supercomputer**, a CRAFT asks for slots from the linked supercomputer with the **most** free slots and spreads
the work over the capable Crafting Computers (the fastest first), as many as the free slots allow. Every supercomputer
has its own queue of slots, **independent** of the others: several supercomputers never block each other's queues. That
is the reason to grow sideways. A network can have any number of supercomputers. The supercomputer and its racks are in
[Servers and racks](servers-and-racks.md); the Cluster Manager runs it.

| Step | Computer | What it does | Key part |
| --- | --- | --- | --- |
| Running | Crafting Computer | Drives the recipes | Crafting Card, Crafting Interfaces |
| Parallelism | Supercomputer | Gives slots for crafts in parallel | Phi coprocessors |

**Choosing the Crafting Computer:** the network keeps the linked Crafting Computers that have the pattern (in a card's
memory, or built in) and sorts them by **crafting speed** (the processors' capacity × the sum of the cards' factors),
from the fastest to the slowest. Without a supercomputer, the first free one takes the craft. Machine recipes live in
the Crafting Interfaces, not only in the computer.

## Crafting coprocessors (Integra Phi)

A supercomputer has **6** coprocessor slots, one per HBW Interface. There is no way past that: it is a hard limit of the
design.

Each Supercomputer Node takes a slot, in the order the High compute fibre reaches it (an Advanced node, with its four
sleds, counts as one slot); a node too many is "unslotted". The crafts each slot runs at once double from slot to slot:

```
crafts in slot N = 8 × 2^(N-1)

slot 1:   8
slot 2:  16
slot 3:  32
slot 4:  64
slot 5: 128
slot 6: 256
-----------
all six: 504 crafts at once per supercomputer
```

Each coprocessor sets up to which slot it serves (Integra, PCIe 3.0, Standard):

| Coprocessor | Cores | GHz | Slots it serves | Most crafts with it |
| --- | ---: | ---: | --- | ---: |
| Integra Phi 5100 | 60 | 1.05 | 1 and 2 | 24 |
| Integra Phi 7120 | 61 | 1.24 | 1 to 3 | 56 |
| Integra Phi 7290 | 72 | 1.50 | 1 to 4 | 120 |
| Integra Phi 9000 | 96 | 1.80 | 1 to 6 | 504 |

A coprocessor in a slot above the ones it serves is "under-rated" and doesn't count. The node has to be assembled with
its bay on; the Cluster Manager shows NO PHI, PHI LOW and OFFLINE. For 504 crafts: 6 nodes with a Phi 9000. Past 504
crafts: more supercomputers, each with its own queue of slots, none blocking the others.

## Where patterns live

- **Bench** patterns live in each **Crafting Card**'s memory: 2 (Vintage), 4 (Legacy), 5 (Transition), 6 (Standard), 8
  (Advanced) per card. A bench pattern goes with its card: taking the card out takes the pattern.
- **Machine** and **multi-stage** patterns live in the **Crafting Interface** of the machine that makes them: 3
  (Vintage), 6 (Legacy), 8 (Transition), 9 (Standard), 12 (Advanced) per interface. The same pattern can be in several
  interfaces; the engine shares the jobs between them.
- What a Crafting Computer can make is what its cards and the interfaces it drives hold. Without a Crafting Card, a
  Crafting Computer doesn't craft. Each card drives as many interfaces as the recipes it holds (2, 4, 5, 6 or 8).

**Media:** a pattern is a `.craft` **file** on a writable medium (a floppy disk, CD-RW, DVD-RW, BD-RE or USB stick), the
same media as the rest of the system; there is no separate "Pattern Disc" item. A medium can hold many patterns
(portable packs of recipes). A pressed CD-ROM can't be written; rewritable media can be written any number of times.

### From the Studio to the network

1. Open the **Pattern Studio** on a computer with a desktop (Frames XP or newer, or a Linux desktop; by Autodeck).
2. Build the recipe (bench, machine or pipeline) and name it if you like.
3. Press Burn. The **Pattern Encoder** linked to the computer writes the file to the medium in its bay.
4. Take the medium to a drive linked to the Crafting Computer.
5. In the **Crafting Manager**, load the file: a bench pattern into a card's memory, a machine or pipeline pattern into
   a Crafting Interface.
6. Ask for the item in the Network Interactor, at the Command Prompt (`operation craft`, `interac craft`) or in IQL
   (`CRAFT 512 piston`).

### The `.craft` file (version 2)

A `.craft` file is SNBT written by the pattern's codec: a bench pattern (nine cells and the result; no type header), a
machine pattern (inputs and outputs as items, fluids or chemicals with amounts, a timeout, a probability per output;
header `type:"proc"`) and a pipeline (ordered stages; `type:"multi"`). A machine pattern doesn't say which machine makes
it: it lives in the interface of the machine that does. Optional fields, left out when empty:

- `name` (up to 64 characters) and `note` (up to 256). A pattern without a name goes by its result. The file name comes
  from the name given (letters, digits and underscores) or from the result's id; two files with the same name on the
  same disk get a suffix (`_2`, `_3`). Nothing is ever written over another file without asking.
- `any`: per bench cell, the item tag the cell takes instead of the exact item (`minecraft:planks`). By default a cell
  is exact; the Studio fills in the tag where the vanilla recipe takes one.

Two patterns are the same recipe when they have the same cells, result and tags; the name and note don't count. In a
cell with a tag, two items of the tag count as the same.

**Resolving tags:** the pattern keeps the tag; the network chooses the item when it plans the craft: the item of the tag
the network has the **most** of (if none, the one drawn). The resolved pattern is what the engine runs; the name, note
and tags stay on the pattern. The Craft Planner, the Crafting Manager and the Network Interactor show the name.

### The Pattern Studio

It keeps nothing on the client: its three drafts live on a pattern bench that belongs to the **machine** (on the
computer; on the item of a rack server), by the same rule as open windows. The window has Bench, Machine and Multi-stage
tabs; a rail on the right with Files (the `.craft` files of every linked drive, and the `crafts/` folder of the system
disk; clicking one opens it in the right editor, or adds it as a stage on the pipeline tab) and Encoder (state, medium,
progress, queue, Cancel, Eject); the player's real inventory under the editor; and the Burn / Save to disk / Load ROM
bar. The game's recipes come through JEI beside the monitor, or are built by hand.

- **Bench:** clicking with an item on the cursor puts a ghost copy; an empty cursor clears; a right click cycles through
  the item's tags and back to the exact item; a cell with a tag shows the item the network would use and its stock.
- **Machine:** inputs and outputs, 3×3 visible of 27 with scrolling; a timeout; a right click on an output cycles its
  probability (100, 75, 50, 25, 10); shift-click opens the amount per cycle (the only place a fluid or chemical amount
  is set); a cell marked as an **estimate** (from a transfer with consumption per tick) stays marked until the author
  confirms it.
- **Pipeline:** "+ Bench" and "+ Machine" push the matching draft as a stage and clear it; Remove; files from the rail
  come in as stages.

Where a draft goes: **Burn** (the linked encoder's queue; the Studio never writes to the medium itself), **Save to
disk** (`crafts/` on the system disk, with a suffix), **Load ROM** (only on a Crafting Computer with a Crafting Card; it
is mirrored in `crafts/`).

**JEI:** with JEI installed, the ingredient list is **always** beside the monitor, on every monitor screen (desktop,
prompt, text console, terminal); the monitor stays centred and JEI fits into the column that is left. The transfer
button puts a crafting recipe on the Studio's bench, already with the tags the recipe takes, and any other recipe in the
machine editor (with fluids and chemicals); an ingredient dragged from the list drops into a cell. Only the Studio
window in front receives; with no Studio open, JEI says "Open the Pattern Studio". Without JEI, recipes are built cell
by cell from the inventory. EMI's list also leaves the monitor screens free.

### The Pattern Encoder

It is the **only** device that writes `.craft` files; drives only read. It is a **peripheral** linked by the peripheral
cable like a drive: it finds its owner through the cable, takes a device port, and disconnects when the cable breaks. It
authors nothing; it receives finished files and writes them. There is one per era, with the real writers' compatibility:

| Block | Era | What it writes |
| --- | --- | --- |
| Vintage Pattern Encoder | Vintage | floppy disks |
| Legacy Pattern Encoder | Legacy | CDs |
| Transition Pattern Encoder | Transition | DVDs and CDs (a DVD writer) |
| Pattern Encoder | Standard | DVDs, CDs and USB sticks |
| Advanced Pattern Encoder | Advanced | Blu-rays and USB sticks |

The medium has to be writable (a pressed CD-ROM is refused), and the block says why it refused. Every file is a **job**
with seek, write and verify phases, and a pause with the result on the display. The fixed part is the drive, not the
file:

| Medium | Seek | Write | Verify |
| --- | --- | --- | --- |
| floppy disk | 20 ticks | 250 bytes per tick | 10 ticks |
| CD | 30 ticks | 1 KB per tick | 10 ticks |
| DVD | 15 ticks | 4 KB per tick | 5 ticks |
| Blu-ray | 12 ticks | 16 KB per tick | 4 ticks |
| USB stick | 4 ticks | at once | 2 ticks |

- While the head is on the medium the bay is locked (neither the slot nor a sneak-click takes it out).
- The file only exists once writing ends; cancelling halfway leaves nothing half written.
- Verify reads it again and compares; if it fails, the display shows the error and the queue carries on.
- A job queued with an empty bay waits for a medium and survives a reload. The queue holds up to eight files.
- The block's panel is only physical: the bay's slot, the link, the era, the medium, the job, a progress bar, Eject,
  Cancel queue.

**Its body:** a GeckoLib model per era, a **full block** (the shell fills the cube, the front details stand 1/32 out
inside it, the medium in the bay reaches the face of the block, the peripheral port takes the last 1/32 at the back),
with the full cube's collision and selection, and no occlusion so it doesn't block its own light. No part ever leaves
the block. What the body shows is bones made visible from the synchronised state: the medium in the bay, one of five
display faces (READY, BUSY/WRITE, OK, ERR, CABLE; the Vintage one shows RD, 88, OK, EE, --), a link lamp and an activity
lamp. The disc spins and the ring and lamp pulse while it writes, with the sounds of the era's writer.

### The Crafting Manager

The Crafting Computer's program (by Autodeck). It lists the `.craft` files on the media in the drives linked to the
computer and the patterns in the cards' memories and in the interfaces, with Load, Load all, Download (writes back to
the medium), Remove and Move (changes place); it is mirrored in `crafts/`. It shows the interfaces with their mode,
their jobs and their pause. Every action needs a Crafting Card (without one, a warning). On MC-NET, the terminal's
Patterns header does the same work.

## The crafting network: interfaces, routers and the receiving bus

Machine recipes live on the **crafting line** (the Crafting Cable, the same in every era, 32 items per tick and a reach
of 16), in three parts.

**The Crafting Interface** (one per era: Vintage, Legacy, Transition, Standard, Advanced) keeps the patterns of **one**
machine (3, 6, 8, 9 or 12 by era) and feeds it: directly when it sits against it, or through a crafting cable of its
own, dyed apart from the main one.

- **Exclusive mode** (one recipe at a time; when the recipe changes, it waits for the machine to empty; the default when
  it feeds through routers) or **shared** (several jobs, different recipes; the default when it feeds directly).
- **Most jobs:** Auto, or from 1 to 64. **Pause.** Two interfaces on the same machine take turns.
- The window shows the machine, the path, the patterns, the inputs and their router, the state, the last jobs, the
  address and the IQL line.

**The Crafting Input Router** (one for every era) sits on an interface's own cable, against an input face of the
machine, for machines with several inputs. It carries **one** input of the recipe, with a filter of up to 5 items. An
input goes through the router chosen for it in the interface's window; otherwise through the first whose filter takes
it; otherwise through the first that takes everything. With no router, the pattern doesn't run.

**The Crafting Receiving Bus** (one for every era) sits against the machine's output. It links itself to the interfaces
of the machine it faces, or by hand to any interface. It credits what comes out to the jobs that fed the machine, never
more than they fed, in the order they were fed. What a job still has to receive when it closes keeps being collected (a
"drain" window). An item no pattern lists goes to the network as unexpected; an undeclared fluid or chemical stays in
the machine.

All three parts carry items, fluids and chemicals. The machine is a **black box**: the Crafting Computer doesn't know
what it is inside. It works with any machine of any mod that has the usual item, fluid, energy and chemical capabilities
(J's Industrial, Mekanism and the rest). Interfaces and routers have names and are set up by name in IQL
(`SET INTERFACE ...`, `SET ROUTER ...`) and in Σ# (`craftInterface("Mixer").Exclusive(true)`,
`craftRouter("North").Only("gravel")`).

**Who drives what:** the Crafting Computers on a crafting cable drive their interfaces, as many as their cards drive, in
the order the cable reaches them. A computer that is off drives none.

**Timeout:** every machine pattern has its own (200 ticks by default). If nothing moves at the interface for that long,
the job ends PARTIAL (if it made something) or FAILED, and the CRAFT says which stage failed. Bench stages give back to
the network what they took for the grid; machine stages leave what they already fed **in** the machine (in the world,
for the player to collect). A recipe no interface has fails at the end of the timeout; one with an interface that can't
feed it (the machine gone, paused, no router) waits.

## Partial crafts

When the plan can't make everything, the craft dialog's Partial button turns amber (only when something can be made):

- the craft runs as far as the ingredients at hand allow: the target drops to the most that can be made;
- the Operation ends COMPLETED_PARTIAL if it delivered something, FAILED if nothing;
- ingredients are **never** wasted: only what was really used is consumed;
- there is no automatic waiting and no retry: the player asks again when they want;
- the dialog and the Craft Planner show what is needed against the stock, with what is missing in red; NextgreIQL notes
  "makes X of the Y asked for". A machine job also closes PARTIAL if it made less.

## Planning and asking

The network engine plans from the bottom up: what the network has, what it has to make first, in what order, on which
machines, and runs the stages on as many machines as it has, at the same time where they don't depend on each other.

The **Craft Planner** (a program) shows the plan before it starts: whether it can be made, the most that can be made,
the stages, the bill of materials and what is missing, and an estimated time. An item with several recipes shows them
side by side with their differences, and the choice is remembered. The **Crafting Manager** shows the jobs while they
run.

A craft is asked for in the Network Interactor, in IQL, at the prompt, in Σ#, and through the ComputerCraft bridge. The
network's buses (Import, Export, External Storage) are in [Storage](storage.md).

## To build

Nothing: autocrafting is built as described here. What is left around it is in the interface (the Craft Planner's Print
and the CRAFT filter, [Interface](interface.md)).

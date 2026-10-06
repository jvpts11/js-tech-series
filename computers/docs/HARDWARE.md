# Hardware

The eras, the computers, and every kind of part they are built from, with what each number on a part's tooltip
means. Numbers are first estimates and are tuned in play.

## Eras

*Added 2026-04-28.*

Every computer and part belongs to an **era**, a generation of computing. Five have content today:

| Era | Like | Word size | Disk space an item takes | Instruction set | Install media |
| --- | --- | --- | --- | --- | --- |
| Vintage | the early 1990s | 16-bit | 1 MB | IA-16 | floppy disks |
| Legacy | around 2000 | 32-bit | 16 MB | x86 | CDs |
| Transition | the late 2000s | 64-bit | 256 MB | x86-64 | DVDs |
| Standard | the 2010s | 64-bit | 256 MB | x86-64 | USB sticks for systems, DVDs for programs |
| Advanced | today | 64-bit | 256 MB | x86-64 | USB sticks for services, Blu-ray for programs |

- An era's computer takes only a motherboard of its own era, and a motherboard seats only parts of its own era.
- A newer era is faster at everything: more cores at higher clocks, more memory, faster disks.
- **Disk space an item takes**: the network keeps items as data on disks, and the era's word size decides how big
  an item is. A 20 MB Vintage drive holds 20 items. From the Transition on an item is 256 MB, so a 1 TB disk holds
  4,096 items.
- **Instruction set** (ISA): what a processor runs. A program built for an instruction set runs on that one and
  every newer one (x86-64 runs x86 and IA-16 programs), never on an older one.
- Each era has its creative tab, **J's Computers - Vintage** to **- Advanced**, with the same shelves in each:
  personal computers, crafting computers, cluster management computers, Mainframes, server racks, rack bays,
  peripherals, network, components and programs. What every era shares is in the tab **J's Computers**.

## The computers

*Added 2026-06-05.*

Every computer needs, to run, a motherboard of its own era, a power supply, at least one processor and at least one
memory module. To show a picture it needs a graphics card (or a processor with graphics built in) and a monitor
joined to it. To do anything useful it needs a disk with a system installed on it ([Systems](SYSTEMS.md)).

| Computer | What it is for | What it holds |
| --- | --- | --- |
| **Personal Computer** | Your own computer: the programs, and your way into the network. | 1 board, 1 CPU, 4 RAM, 4 GPUs, 2 disks, 1 PSU (the board may offer fewer), and 18 storage slots. |
| **Crafting Computer** | Runs the network's crafting ([Autocrafting](AUTOCRAFTING.md)). Needs a Crafting Card. | 1 CPU, 4 RAM, 4 expansion cards, 2 disks, 1 PSU. |
| **Cluster Management Computer** | Installs systems on racked machines and watches them. Without a Cluster Interface Card it is an ordinary computer. | 1 CPU, 4 RAM, 4 expansion cards, 2 disks, 1 PSU, 18 storage slots. |
| **Mainframe** | The network's brain: it keeps the index of everything stored, moves it and crafts it. | A block of 3 wide, 2 tall, 2 deep. 1 MTX board, 1 PSU, 4 CPUs, 8 RAM, 6 GPUs, 4 disks, 27 storage slots. |
| **Server Rack** | A cabinet of 8 rack units (U) for Servers, a KVM Switch, a Rack UPS and a Cooling Unit. | A block of 2 wide, 3 tall, 2 deep. |
| **Supercomputer Rack** | The same cabinet for Supercomputer Nodes only (Standard and Advanced). | As the Server Rack. |

Each computer exists in every era, named for it (the Vintage Personal Computer, the Legacy Mainframe). From the
Standard on, the Personal Computer and the Crafting Computer each come in three cases that differ only in look:
the plain one, "High Performance" and "Aesthetic".

The board a case takes depends on its size (its **form factor**): a Vintage Personal Computer takes a Baby-AT or
AT board, a Legacy one an ATX board, and from the Transition on an ATX or EATX board.

**Why the Mainframe's hardware matters most.** Everything the network does goes through the Mainframe:

- its **processors** decide how many items it handles at once (its capacity, in items a tick);
- its **memory** is the buffer items wait in on their way in or out;
- each **graphics card** gives it one more queue of work done at the same time as the others (a Mainframe with 3
  GPUs works 4 queues), and more monitors.

### Servers and their racks

*Added 2026-06-06.*

A rack seats servers of its own era or older. Each server has a height in rack units, drive bays at the front of
the rack, slots for gadgets, and room for processors and expansion cards:

| Server | U | Drives | Gadgets | Most CPUs | Most cards |
| --- | --- | --- | --- | --- | --- |
| Vintage Server | 1 | 1 | 0 | 1 | 1 |
| Legacy Server | 1 | 2 | 1 | 2 | 1 |
| Transition Server | 1 | 4 | 1 | 2 | 2 |
| Server (Standard) | 1 | 3 | 1 | 2 | any |
| Advanced Server | 1 | 4 | 1 | 2 | any |
| Storage Server | 2 | 8 | 2 | 1 | 1 |
| Compute Server | 2 | 1 | 1 | 4 | any |
| Supercomputer Node | 2 | 1 | 1 | 2 | 2 |
| Advanced Supercomputer Node | 2 | 2 | 1 | 2 | 4 |

- **Gadgets**: a **RAID Controller** joins a server's drives (RAID 0 with at least 2 drives: a quarter faster,
  nothing kept if one fails; RAID 1 with at least 2: every drive a copy; RAID 5 with at least 3: survives losing
  one), and a **Cache Card** cuts how long a read waits.
- **Rack units**: the **KVM Switch** lets one monitor reach several machines; the **Rack UPS** and the **Cooling
  Unit** fill the cabinet's other bays.
- A **supercomputer** is every Supercomputer Rack tied together by the high compute cable, meeting the network at
  one **HBW Interface**.

## Parts

*Added 2026-06-13.*

Hover a part to read its tooltip. What each line means:

### Processors (CPU)

170 of them across the eras, from the Integra and Velocion makers.

- **N cores @ X GHz**: how many things it does at once, and how fast each.
- **Architecture**: its design; a newer design does more in a tick at the same clock.
- **N it/t**: its capacity, in items a tick (cores times clock times design). In a Mainframe this is how many
  items the network handles at once. **W**: the power it draws, in watts.
- **Socket**: the board socket it fits. A board names its socket too; they must match.
- **x86-64, 64-bit, 256 MB per item**: its instruction set, its word size, and the disk space an item takes in its
  era.
- A processor with graphics built in says so: it lights a monitor without a graphics card.

### Memory (RAM)

29 modules, from SIMM and EDO in the Vintage to DDR5.

- **N items buffer**: how many items it holds on their way in or out of the network, in a Mainframe.
- Its generation (SDRAM, DDR3...): a board takes only some generations, and says which.
- Memory also holds the programs a computer runs: each program and each system needs some.

### Graphics cards (GPU)

76 of them, from the VGA-256 to the Envya Vertex RTX 5090.

- **N cores, M MB VRAM**: its power and its video memory. Every monitor, and every window that draws a picture
  of its own, uses some video memory; a bigger monitor more.
- **+1 parallel queue**: in a Mainframe, each card adds one queue of work.
- A card in a slot older than itself runs at that slot's speed, and the tooltip says so.

### Motherboards

49 boards. The tooltip says its form factor (which cases it fits), its sockets, how many memory modules and of
which generations, its expansion slots and their generation, sound on the board when it has it, and its era. A
board seats parts of its own era only.

### Power supplies (PSU)

11 of them, 200 W to 3,000 W, with their efficiency. A computer whose parts draw more than its power supply gives
refuses to start. Some power supplies scale to what the computer draws and never refuse.

### Disks

| Era | Disks |
| --- | --- |
| Vintage | Vaultis Trench HDD, 20 MB, 100 MB, 200 MB |
| Legacy | Vaultis Link IDE HDD, 4 GB, 20 GB, 40 GB |
| Transition | Vaultis Link SATA SSD 64 GB; Vaultis Keep HDD 500 GB, 1 TB, 2 TB |
| Standard | Vaultis Keep HDD 4 and 8 TB; Vaultis Swift SSD 500 GB to 4 TB; Vaultis Bolt NVMe 500 GB to 2 TB |
| Advanced | Vaultis Keep HDD 12 to 24 TB; Vaultis Swift SSD 8 TB; Vaultis Bolt NVMe 4 and 8 TB |

- **Capacity in items**: what the disk holds, counted in items of its era.
- **HDD, SSD, NVMe**: the kind, and with it the speed. An SSD moves data four times as fast as a hard disk, NVMe
  sixteen times; before a transfer starts a hard disk waits 10 ticks, an SSD 3, NVMe 1 (server settings can
  change these).
- Once a system is installed, the tooltip says which, its desktop, and how many programs.

### Cards

- **Sound cards** (the Artisan Tone Blaster family, Vintage to Transition): one a computer, of the board's era.
  From the Transition on, boards have sound of their own.
- **Crafting Cards**: what makes a Crafting Computer craft. Each drives a number of Crafting Interfaces and keeps
  that many crafting table recipes in its own memory: 2 in the Vintage, 4 in the Legacy, 5 in the Transition, 6
  in the Standard, 8 in the Advanced.
- **Workshop cards** (Crafting Table, Furnace, Enchanting, Anvil): let a Personal Computer's Workshop program
  craft, smelt, enchant and repair, and let the network change stored items the same way.
- **Cluster Interface Cards** (Serial Console Card, Management NIC, Fabric Host Adapter): let a Cluster Management
  Computer reach racked machines; each newer one reaches more, and installs on several at once.
- **Optical Network Card**: lets a machine take fibre on the backbone.
- **Integra Phi** coprocessors (four of them), for supercomputer nodes.

## What can go wrong

- **The window refuses the build.** It names the reason: no CPU or no RAM; more of a part than the board has
  slots for; a CPU that does not fit the socket; processors of two instruction sets mixed; a card in the wrong kind
  of slot; memory of a generation the board does not take; two sound cards, or one of another era; more power
  drawn than the power supply gives.
- **A part sits in the window but does nothing.** The board does not offer that slot. Use a board with more.
- **A part does not go in at all.** It is from another era than the board.
- **The network is slow.** Look at the Mainframe first: its processors set how much it handles at once, and its
  graphics cards how many queues it works.

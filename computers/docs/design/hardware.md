# Hardware

Processors, memory, graphics cards and motherboards: what each part is, and what it gives the computer it goes into.
Every part of every era, with its numbers, is in [the catalogue](catalogue.md), generated from the code. Disks are in
[Storage devices](storage-devices.md) and power supplies in [Power](power.md).

## Processors

A processor has an era, an instruction set, a socket, a number of cores, a clock, a power draw (TDP) and a **design**:
the microarchitecture its cores are built on (Haswell, K8, Way 3), with the codename of the chip itself (Devil's Canyon,
Conroe). A design can give each core two threads (SMT), and a hybrid chip has, next to its performance cores,
**efficiency cores** with a design and a clock of their own. Three processors carry graphics on the die (below).

### Capacity

How many items a processor handles per tick:

```
capacity = 40 × (P cores × GHz × efficiency × thread factor + E cores × E GHz × E efficiency)
```

- The thread factor is 1.2 with two threads per core (SMT), and 1 without.
- It is rounded to the nearest whole item, and is never below 1.
- A computer's capacity is the sum of every processor on its motherboard.
- The 40 is a constant of the code; the balance is tuned through the efficiency table.

A design's **efficiency** is the work one of its cores does per gigahertz, measured against the P6 design of the late
90s (1.0). It is what stops an old chip at a high clock from overtaking a better one that came later:

| Design | Efficiency |
| --- | ---: |
| 486 | 0.45 |
| P5, K5 | 0.6 |
| NetBurst | 0.7 |
| K6 | 0.75 |
| P6 | 1.0 |
| K7 | 1.1 |
| K8 | 1.3 |
| Bulldozer, Piledriver | 1.35 |
| K10 | 1.5 |
| Centro | 1.6 |
| Nehalem, Westmere | 2.0 |
| Gracemont (efficiency cores) | 2.3 |
| Sandy Bridge, Ivy Bridge | 2.4 |
| Haswell | 2.7 |
| Way 1, Way 1+ | 2.7 |
| Skylake-SP, Cascade Lake | 2.8 |
| Skylake, Kaby Lake, Coffee Lake, Comet Lake | 2.9 |
| Skymont (efficiency cores) | 2.9 |
| Way 2 | 3.1 |
| Ice Lake-SP, Cooper Lake | 3.1 |
| Sapphire Rapids, Emerald Rapids | 3.4 |
| Way 3 | 3.5 |
| Alder Lake, Raptor Lake | 3.6 |
| Way 4 | 3.8 |
| Arrow Lake | 3.9 |
| Way 5 | 4.1 |

Two examples:

- **Integra Centro c7 4790K:** 4 cores at 4.0 GHz on Haswell, with SMT: 40 × 4 × 4.0 × 2.7 × 1.2 = 2,073.6, so 2,074
  items per tick.
- **Integra Centro c5 13600K:** 6 performance cores at 3.5 GHz on Raptor Lake, with SMT, and 8 efficiency cores at 2.6
  GHz on Gracemont: 40 × (6 × 3.5 × 3.6 × 1.2 + 8 × 2.6 × 2.3) = 5,542.4, so 5,542 items per tick.

Across the eras, capacity goes from 1 (an Integra 486SX at 25 MHz) to 43,776 (a Velocion Threadkiller 7995WX, 96 cores).

What capacity is for depends on the computer:

- On a **Mainframe**, its processors' capacity is how many items the network handles at once ([Computers](computers.md),
  [Operations](operations.md)).
- On a **server**, it is how fast the server sends out what it stores ([Servers and racks](servers-and-racks.md)).
- On a **Crafting Computer**, its crafting speed is its capacity times the factors of its Crafting Cards
  ([Autocrafting](autocrafting.md)).
- What a processor gives **programs** is a different budget, worked out from its cores and clock ([Σ and Σ#](sigma.md)).

**Variety.** Every era has several sockets, and on each, per brand, at least an entry-level model, a middle one and a
top one, plus the server and workstation lines where the era had them. Building a strong computer and a weak one in the
same era should feel different, and the player is never pushed to the most powerful processor: weaker options are the
right choice for computers with little to do.

### Sockets

A processor fits a motherboard when both name the same socket. A socket is an id (`namespace:path`), so a mod that
brings processors can bring the socket they sit in.

| Era | Sockets |
| --- | --- |
| Vintage | Socket 3, Socket 7, Socket 8, Slot 1 |
| Legacy | Socket 370, Socket A, Socket 478, LGA 775, Socket 754, Socket 939, Socket 604, Socket 940 |
| Transition | LGA 775, AM2, AM3, LGA 1156, LGA 1366, LGA 771, Socket F |
| Standard | LGA 1155, LGA 1150, LGA 2011, AM3+, FM2+, G34 |
| Advanced | AM4, AM5, LGA 1151, LGA 1200, LGA 1700, LGA 1851, LGA 2066, sTR4, sTR5, SP3, SP5, LGA 3647, LGA 4189, LGA 4677 |

### Instruction sets

The instruction set (ISA) is what a processor understands, and so which programs it runs. It is not the design: two
chips of the same instruction set can be built in very different ways.

| ISA | Word | Runs programs built for | Processors of |
| --- | --- | --- | --- |
| IA-16 | 16 bits | IA-16 | Vintage |
| x86 | 32 bits | x86 and IA-16 | Legacy |
| x86-64 | 64 bits | x86-64, x86 and IA-16 | Transition and later |

- A program runs on the ISA it was built for and on every newer one, never on an older one: a Vintage computer runs the
  programs of its time and nothing newer.
- A machine with processors of two ISAs is refused.
- A Σ program is built for IA-16 and a Σ# program for x86, so each runs on the oldest machine it can
  ([Σ and Σ#](sigma.md)). The compiler only moves a program up to a newer ISA if it uses an instruction the older one
  does not have. Today the 32-bit and 64-bit ISAs have the same instructions, and new instructions go into the 64-bit
  one only.
- The ISA does not follow the era: the mod's own processors line the two up, but a mod may bring a 64-bit processor of
  an early era, or the other way round, and may register ISAs of its own ([The API](api.md)).

### Graphics on the die

Three processors carry graphics on the die, like the Haswell desktop chips: the Integra Centro c5 4590, c5 4690K and c7
4790K, with Integra HD Graphics 4600 (20 units, up to 1,150, 1,200 and 1,250 MHz). A machine with one of them and no
graphics card lights a monitor through the motherboard's own video output, and the graphics use the system's memory: a
quarter of it, 1,792 MB at most. These graphics never run a desktop's effects. With a graphics card installed, the card
draws the screens.

### Enigmatic hardware

The hardware unlocked through J's Space's research system belongs to J's Space, and only exists when J's Space and J's
Computers are installed together ([The series](series.md)). It is designed with J's Space; J's Computers has no rules of
its own for it.

## Memory

In memory, **one item is 4 MB**, so the items a module holds are its gigabytes × 256. (On a disk an item costs something
else, by the disk's era: [Storage devices](storage-devices.md).)

Memory has two jobs:

1. **The buffer.** Items on their way into or out of the network wait in memory. A computer's buffer is the sum of its
   modules' buffers, and the Mainframe moves per tick at most what the buffer holds: it works at the smaller of its
   processors' capacity and its buffer. If the buffer is smaller than the capacity, the processors sit idle waiting for
   memory, so sizing memory for the processors installed is one of the basic hardware decisions of the mod. For example,
   an Advanced Mainframe with 4 Velocion Epic 9654 (42,025 items per tick each, 168,100 in total) needs at least 168,100
   items of buffer. With DDR5-131072 RDIMM modules (32,768 items each) that is 6 modules; the Mainframe counts 8, so its
   maximum is 8 × 32,768 = 262,144 items.
2. **Working memory.** Memory is also where the computer's own work lives. The operating system, the desktop, every
   service, every program running, every open window and every Σ# process takes megabytes, in a **memory ledger** per
   computer, kept on the server. A computer's total memory is the sum of its modules. A program only opens if the memory
   it *promises* to need fits beside what is already promised; the screens show what is really *held* at that moment,
   which is usually less. Every system's Task Manager and System Monitor read the same ledger ([Programs](programs.md),
   [Operating systems](operating-systems.md)). Graphics on the die take their memory from here.

Memory never stores items as storage. Fast reads of the most requested items are the job of the Predictive Cache and the
Cache Card ([Servers and racks](servers-and-racks.md)), which never lose an item.

**Generations don't mix.** SIMM, EDO, SDRAM, DDR, DDR2, DDR3, DDR4 and DDR5 are all incompatible with each other. A
motherboard lists the generations it takes, and a module has to be on that list and of the board's era. Changing eras
means changing modules.

**Latency by generation.** Before a transfer starts, it waits for memory. A computer waits as long as its fastest module
does.

| Generation | Eras | Wait |
| --- | --- | --- |
| SIMM | Vintage | 5 ticks |
| EDO | Vintage | 4 ticks |
| SDRAM | Vintage, Legacy | 3 ticks |
| DDR | Legacy | 2 ticks |
| DDR2 | Legacy, Transition | 2 ticks |
| DDR3 | Transition, Standard | 1 tick |
| DDR4 | Advanced | 1 tick |
| DDR5 | Advanced | 0 |

A module's frequency doesn't enter the sums: the generation and its wait already stand for it. The DDR6 and HBM
generations exist in the code, with no modules, waiting for the Exa and Singularity hardware.

## Graphics cards

A graphics card has an era, the slot it was made for, a number of cores, a clock, video memory (VRAM), a power draw and
a design with its chip's codename (Kepler GK110, TeraScale RV770). What a card counted as a core changed over the years
(pipelines on the first ones, shaders later), and the designs that worked their shaders in groups of five (TeraScale and
TeraScale 2) are counted one group at a time.

What a graphics card does depends on the computer:

- **Every computer:** the video outputs monitors plug into, and the VRAM monitors and graphical windows use.
- **Mainframe and Subframe:** every card adds one more parallel, independent queue of Operations (below).
- **Server:** a transmission bonus of threads × 0.05 items per tick, added to the processors' capacity. It complements
  the processors and never dominates them: an Envya Vertex RTX 4090 has 65,536 threads, so +3,277 items per tick.

A card's threads are its cores × 4.

### Power

What a card can do, in the same items per tick as a processor's capacity:

```
power = units × GHz × efficiency
```

- Units are the cores, or the groups of five on TeraScale designs.
- It is rounded, and never below 1.
- Graphics designs' efficiencies are on the same scale as processors', so that the fastest card of an era does a little
  more than the fastest processor of it. The big swings follow what a card counted as a core (a 90s card listed one or
  two pipelines; a modern one, thousands of shaders). Tesla and Fermi count at their shader clock, double the rest of
  the chip.

| Design | Efficiency | Design | Efficiency |
| --- | ---: | --- | ---: |
| VGA | 10 | Tesla | 2.4 |
| Rendition | 60 | TeraScale (per group) | 6.5 |
| NV3 | 20 | Fermi | 1.8 |
| Rage | 40 | TeraScale 2 (per group) | 5.0 |
| Voodoo | 30 | GCN | 1.0 |
| Fahrenheit | 36 | Kepler | 1.0 |
| Celsius | 25 | Maxwell, Pascal | 1.35 |
| R100 | 50 | Volta | 1.6 |
| R200 | 25 | Turing | 1.85 |
| Kelvin | 30 | RDNA | 1.75 |
| R300 | 25 | Ampere, Ada Lovelace, Hopper, Blackwell | 1.35 |
| Rankine | 13 | RDNA 2, RDNA 3 | 2.0 |
| Curie | 23.5 | RDNA 4 | 2.1 |
| R400 | 19 | | |
| R500 | 22 | | |

For example, an Envya Vertex RTX 4090, 16,384 cores at 2,235 MHz on Ada Lovelace: 16,384 × 2.235 × 1.35 = 49,435.

### The Mainframe's queues

The Mainframe works one queue of Operations with its processors and one more per graphics card: a Mainframe with 3 cards
works 4 queues at once. The processors' queue runs at the processors' capacity. A card's queue runs at the **smaller**
of the processors' capacity and the card's power, cut further by its slot if the card is newer than the slot (below),
and never below 1. A strong card runs its queue at the processors' speed; a weak one runs it slower, and the Mainframe
shows it in amber. Queues do **not** share the capacity between them ([Operations](operations.md)). The practical limit
is how many cards the Mainframe counts (6) and how much power they draw.

### Video memory

A machine's VRAM is the sum of its cards' VRAM, each cut by its slot like its power. It is spent like memory:

- every **monitor** that is on holds the VRAM of one block of its era, times the blocks of its screen (a wall of several
  monitors counts them all): 64 KB for a one-colour Vintage tube, 256 KB for a sixteen-colour one, 4 MB Legacy, 16 MB
  Transition, 64 MB Standard, 256 MB Advanced;
- every **window that draws an image of its own** (a Σ# canvas, Paint) holds a quarter of a colour monitor block of the
  machine's era, or a whole block while it fills the screen.

Windows count first, then monitors in the order they were linked. A monitor that doesn't fit stays dark and says how
much it needs and how much is free. A server board (EEB) has a console output with its own memory for one monitor of its
era. A machine whose only graphics are on the processor's die uses the system's memory.

### Video outputs

A monitor takes one video output. A graphics card has 1 (Vintage), 2 (Legacy and Transition) or 4 (Standard and
Advanced), by the card's own era. A server board (EEB) adds its console output, and a processor with graphics on the die
gives the motherboard one output. There is no other limit on monitors.

### Slot bandwidth

Slots come in four families, and a card goes in a slot of its own family:

| Family | Generations, oldest to newest |
| --- | --- |
| ISA | ISA |
| PCI | PCI |
| AGP | AGP 2x, AGP 4x, AGP 8x |
| PCI Express | PCIe 1.0, 2.0, 3.0, 4.0, 5.0 (6.0 reserved) |

Along a family's generations, each has about twice the bandwidth of the one before. A card in a slot of its own
generation or newer runs in full. A card in an older slot works, held back by the slot: half the bandwidth one
generation behind, a quarter two behind, never less than an eighth. The cut applies to a graphics card's power on its
queue and to its VRAM, and to how many machines a Cluster Interface Card installs on at once
([Servers and racks](servers-and-racks.md)). The tooltip of a card held back by its slot says so.

### Desktop effects and the Experience Index

The desktops that drew their effects on the graphics card (Frames 7, Frames 10, Frames 11 and Cinnamon always; KDE
Plasma from the Transition on and GNOME from the Standard on) need a capable card; on a weaker one they run their
**basic look**. Capable means a graphics score of at least 3.0 on the **Experience Index**, the rating Frames 7 shows
for a machine:

- five scores from 1.0 to 7.9: processor, memory, desktop graphics, gaming graphics and main disk; the machine's **base
  score** is the lowest of the five;
- each score grows with the logarithm of what the part does, so doubling a part adds about one point;
- the graphics scores are capped by the card's VRAM: under 128 MB 2.9, under 256 MB 4.9, under 512 MB 5.9, under 1 GB
  6.9;
- graphics on the die never pass 2.9;
- the disk is worth 5.6 as a hard disk, 7.4 as an SSD and 7.9 as NVMe.

The Task Manager and the System Monitor show the graphics card's part (VRAM in use, load).

### Compute cards

The compute cards (the Envya Tessera K40, V100, A100 and H100, and the MI300X) are datacenter GPUs. They belong to the
AI: they go on an AI Server's GPU baseboard, pool their VRAM, train and generate ([Artificial intelligence](ai.md)).
Mining is in [Teracoin](teracoin.md).

## Motherboards

- A motherboard has an era, a form factor, one kind of socket and a number of sockets, the memory generations it takes
  and its number of memory slots, its expansion slots, and its number of disk slots. Every board follows the real
  chipset of its socket: the memory and the graphics bus that chipset had.
- The form factor decides which case the board goes in ([Computers](computers.md)): Baby-AT and AT (Vintage computers),
  ATX (Personal Computer, Crafting Computer and Cluster Management Computer from the Legacy on), EATX (workstations:
  Cluster Management Computer from the Legacy on, Personal Computer from the Transition on, and servers), EEB (servers)
  and MTX (Mainframes).
- The board **offers** slots; the case **counts** at most its own: a Mainframe uses 4 processors, 8 memory modules, 6
  cards and 4 disks, even on an MTX board with 64 memory slots. A slot the board doesn't offer stays closed. A Mainframe
  grows sideways, through Subframes, not up, through bigger boards.
- Sockets belong to their family: a socket only takes the processors that use it (SP3 only Epic, AM4 only Sprint and
  Awayken, sTR5 only Threadkiller). They are not interchangeable.
- **Ports** follow the board's era, in three kinds, as on a real computer:
  - **device ports** on the board: 2 (Vintage), 4 (Legacy), 6 (Transition), 8 (Standard), 10 (Advanced), for printers,
    drives, hubs, Redstone Interfaces, Network Gateways and the rest;
  - **video outputs**: the graphics cards', an EEB board's console output, and a processor's with graphics on the die;
    they serve monitors;
  - **audio outputs**: the sound card's and, from the Transition on, the motherboard's own; each serves a pair of
    speakers. Ports are physical in the world, joined by the peripheral cable ([Peripherals](peripherals.md)).
- **Sound:** from the Transition on, boards have their own sound (the HD Audio of the late 2000s); before that, only the
  speaker in the case, unless there is a sound card ([Sound](sound.md)).

The number of device ports is how many peripherals (other than monitors and speakers) can be linked at once. A
peripheral right against the computer needs no cable. A player who needs more peripherals than ports uses a **hub** of
the era: it takes one device port and offers 2 (Vintage), 4 (Legacy and Transition) or 7 (Standard and Advanced); the
cable after a hub reaches as far again, and hubs hang from hubs. Monitors and speakers don't go through hubs.

The mining and simulation boards are designed with their systems ([Teracoin](teracoin.md),
[The cosmological simulator](simulator.md)).

## To build

- **Expansion slots in groups** on every motherboard: see [Eras](eras.md). Today each board has one kind of slot.
- **Compute cards without video outputs**, on the AI Server's GPU baseboard: see [Artificial intelligence](ai.md). Today
  the Tessera cards work as ordinary graphics cards; the MI300X and the NPUs only exist as art.

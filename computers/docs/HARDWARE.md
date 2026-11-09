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
| **Personal Computer** | Your own computer: the programs, and your way into the network. | 1 board, 1 CPU, 4 RAM (2 and 8 on a two-socket board), 4 GPUs, 2 disks, 1 PSU (the board may offer fewer), and 18 storage slots. |
| **Crafting Computer** | Runs the network's crafting ([Autocrafting](AUTOCRAFTING.md)). Needs a Crafting Card. | 1 CPU, 4 RAM, 4 expansion cards, 2 disks, 1 PSU. |
| **Cluster Management Computer** | Installs systems on racked machines and watches them. Without a Cluster Interface Card it is an ordinary computer. | 1 CPU, 4 RAM (2 and 8 on a two-socket board), 4 expansion cards, 2 disks, 1 PSU, 18 storage slots. |
| **Mainframe** | The network's brain: it keeps the index of everything stored, moves it and crafts it. | A block of 3 wide, 2 tall, 2 deep. 1 MTX board, 1 PSU, 4 CPUs, 8 RAM, 6 GPUs, 4 disks, 27 storage slots. |
| **WAN Gateway Computer** | Stands at either end of a long-distance line, a port on its back for each of its WAN Interface Cards. It joins its own network through the backbone, as the Mainframe does. | A block of 4 wide, 2 tall, 2 deep. 1 MTX board, 1 PSU, 4 CPUs, 8 RAM, 6 expansion cards (up to 4 of them WAN Interface Cards), 4 disks. |
| **Server Rack** | A cabinet of 8 rack units (U) for Servers and the rack's own units: a KVM Switch, a Rack UPS and its battery expansions, a PDU, a Cooling Unit, a Vent Panel, a Drive Shelf. | A block of 2 wide, 3 tall, 2 deep. |
| **Supercomputer Rack** | The same cabinet for Supercomputer Nodes only (Standard and Advanced). | As the Server Rack. |

Each computer exists in every era, named for it (the Vintage Personal Computer, the Legacy Mainframe). From the
Standard on, the Personal Computer and the Crafting Computer each come in three cases that differ only in look:
the plain one, "High Performance" and "Aesthetic".

The board a case takes depends on its size (its **form factor**): a Vintage Personal Computer takes a Baby-AT or
AT board, a Legacy one an ATX board, and from the Transition on an ATX or EATX board. The Cluster Management
Computer takes EATX from the Legacy on; the Crafting Computer never does.

**Two-socket workstations.** An EATX board may carry two sockets: the Legacy's on Socket 604 and 940, the
Transition's on LGA 771, the Standard WS on LGA 2011, and the Advanced WS on LGA 4677 and SP5. On one, a Personal or
Cluster Management Computer counts two processors and eight memory modules, as a real two-socket workstation does:
its assembly screen opens a second row under the first, for the second processor and modules 5 to 8. A board with
one socket uses the first row only, however many memory slots it has.

**Why the Mainframe's hardware matters most.** Everything the network does goes through the Mainframe:

- its **processors** decide how many items it handles at once (its capacity, in items a tick);
- its **memory** is the buffer items wait in on their way in or out;
- each **graphics card** gives it one more queue of work done at the same time as the others (a Mainframe with 3
  GPUs works 4 queues), and more monitors.

### Servers and their racks

*Added 2026-06-06.*

A rack seats servers of its own era or older. Each server has a height in rack units, drive bays at the front of
the rack, and room for processors and expansion cards:

| Server | U | Drives | Most CPUs | Most cards |
| --- | --- | --- | --- | --- |
| Vintage Server | 1 | 1 | 1 | 1 |
| Legacy Server | 1 | 3 | 2 | 2 |
| Transition Server | 1 | 5 | 2 | 2 |
| Server (Standard) | 1 | 4 | 2 | any |
| Advanced Server | 1 | 5 | 2 | any |
| Vintage Storage Server | 2 | 5 | 1 | 2 |
| Legacy Storage Server | 2 | 7 | 1 | 2 |
| Transition Storage Server | 2 | 8 | 1 | 2 |
| Storage Server (Standard) | 2 | 10 | 1 | 2 |
| Advanced Storage Server | 2 | 10 | 1 | 2 |
| Vintage Compute Server | 2 | 1 | 2 | 1 |
| Legacy Compute Server | 2 | 2 | 2 | 2 |
| Transition Compute Server | 2 | 2 | 4 | 2 |
| Compute Server (Standard) | 2 | 2 | 4 | any |
| Advanced Compute Server | 2 | 3 | 4 | any |
| Supercomputer Node | 2 | 2 | 2 | 2 |
| Advanced Supercomputer Node | 2 | 3 | 2 | 4 |

- **Storage cards**: Vaultis makes two for each era's slot, from ISA to PCI-e 4.0. The **Vaultis Array** joins the
  drives of the server it sits in (RAID 0 with at least 2 drives: a quarter faster, nothing kept if one fails;
  RAID 1 with at least 2: every drive a copy; RAID 5 with at least 3: survives losing one), and the **Vaultis
  Cache** cuts how long a read waits, from 10% on the ISA card to 30% on the PCI-e 4.0 one.
- **Rack units**: the **KVM Switch** lets one monitor reach several machines; the **Rack UPS** carries the cabinet
  through an outage (2.7 MJ for up to 3 kW, and each **UPS Battery Expansion** adds 2.7 MJ; see
  [UPSes](#upses)); the **PDU** measures every bay's draw and lets the network switch the bays (`QUERY power`,
  `UPDATE power SET on = no WHERE server = 'VAULT-01'`); the **Drive Shelf** (2U, Standard) adds ten drive bays to
  the server mounted right above it, and does nothing with no server there; the **Cooling Unit** adds to the heat
  the cabinet can carry, 300 W for the Vintage one, 600 W for the Legacy, 1,000 W for the Transition, 1,500 W for the
  Standard and 2,500 W for the Advanced, and the **Vent Panel** 500 W, one for every era in its rack's colour (see
  [Cooling](#cooling)). The KVM Switch, the Rack UPS, the Cooling Unit, the battery expansion and the PDU come in every
  era, and a rack seats the units of its era and the earlier ones, as it does servers. The
  **Fibre Optic Interface** is how a rack takes the fibre backbone: the Standard's takes the Fibre Optic Cable, the
  Advanced's, in an Advanced rack, the OM5 Fibre Cable as well.
- A **supercomputer** is every Supercomputer Rack tied together by the high compute cable, meeting the network at
  one **HBW Interface**.
- A **Server Router** groups the racks behind each of its faces into a datacenter section (added 2026-11-08: one per
  era from the Legacy on). It takes the data cables of its era and the earlier ones, and manages racks across all its
  sections by its era: 4 for the Legacy's, 8 for the Transition's, 16 for the Standard's and 32 for the Advanced's.
  Its window shows the era in its corner and the racks against that budget. The Vintage has none: its racks join the
  backbone directly.

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

*Added 2026-11-08.* A running computer draws its energy from the grid through its power supply: its parts' watts
over the supply's efficiency (300 W of parts on an 80% supply take 375 W from the line). It takes J's Energy in
the low voltage class, or FE while J's Core lets the series take it (the default), from any cable or block that
gives energy. Without energy it does not start; when energy stops reaching it, it goes dark at once, as a computer
whose plug is pulled does, and the network's event log says so in an ERROR line. A rack's powered bays draw the
same, each machine through its own supply, from the cabinet. The assembly screen's DRAW readout shows the parts
against the supply, its bar amber near the rating and red past it, and its tooltip the watts from the line (NO
POWER when nothing reaches the computer). The server setting `power.draw_energy` turns the draw off, for a world
with no energy at all.

*Added 2026-11-08.* A power supply wears out while it is on, and in the end fails. The wear is kept on the supply,
so it goes with it from machine to machine, and how fast it grows depends on the load (the parts' watts over the
rating): up to 50% a supply lives eight times its full-load life, up to 80% three times, above that its full-load
life. At full load an MF PowerBasic lasts 60 game days, a PowerGold 100, a PowerPlat 150 and a ServerPSU 250, each
supply a little more or less (up to a fifth either way, drawn once); a computer or a rack slowed by its heat wears
its supplies twice as fast ([Cooling](#cooling)). The wear shows as a bar under the supply in the assembly screens,
as an item's durability does, and in its tooltip and the DRAW readout's. From 80% worn the supply is degraded: its seat's edge turns amber,
the self-test of every boot writes a warning, the Device Manager marks it under Power supplies, the firmware's
Hardware page shows it, and the network's event log records a WARN. At 100% it fails: a system that is up goes down
on its red page, which names the supply, and the computer goes dark (an ERROR in the log); it does not start again,
the assembly screen saying "PSU failure", until the supply is replaced; a failed supply cannot be repaired. The MF
ServerPSU 3000P (Redundant) has two units that share the load, each with its own wear: when one fails, the other
carries everything and wears faster for it, and only with both failed does the server go dark. The server settings
`hardware.psu_failures` (off, no supply wears) and `hardware.psu_life_multiplier` (0.1 to 10) change all of this.

*Added 2026-11-08.* A computer that loses its power instead of being switched off shuts down **suddenly**: a power
loss, unstable power or a failing supply. Unstable power is a line that still brings something, only less than the
computer draws (a weak generator, an overloaded grid): a system that is up goes down on its red page first, with the
stop code INTERNAL_POWER_ERROR on Frames and each Unix's own panic, and the computer is dark after it. A line cut
altogether takes it at once, with no page. What the Operations of a Mainframe that goes dark were carrying (in its
memory buffer, on their way to a disk, to an inventory or to a player) is lost, and so is the work in a Personal
Computer's card when that computer goes dark; the network's event log says how much and of what. The server setting
`item_loss.power_loss` gives it all back instead. The disks the system had in use are checked the next time it comes
up ([Systems](SYSTEMS.md#after-a-sudden-shutdown)). A rack's bays shut down suddenly the same way. The Local heading
of a computer's monitor shows its DRAW (its parts' watts) and its LINE (what it takes from the line, in FE a tick),
and its id across from its title, which tells it from machines of the same name.

### UPSes

*Added 2026-11-08.* A UPS keeps computers running when the power goes. The **MF PowerGuard** is a whole block, one
for every era: the 300 of the Vintage and the Legacy (90 kJ, up to 300 W), the 500 of the Transition (240 kJ,
500 W), the 800 of the Standard (480 kJ, 800 W) and the 1500 of the Advanced (1.35 MJ, 1,500 W). The energy cable
plugs into the inlet in the middle of its back or of either side, and every computer standing against it, on any
face, draws from it. While the line brings what they draw, it feeds them and charges its battery on top (an empty
battery is full in ten game minutes); when the line goes or falls short, the battery carries them, and the amber lamp
lights; above what it carries it is in bypass, its green lamp blinking, and the computers run straight from the line,
unprotected. A line one class too high, or a lightning surge, does not get through it: it takes nothing from that line
and carries the computers on its battery; two classes too high, not even a UPS holds. Looking at it says how it
stands and how long the battery lasts.

The **Rack UPS** (1U) does the same for its whole rack: 2.7 MJ for up to 3 kW, and every **UPS Battery Expansion**
in the rack adds 2.7 MJ more. The rack's screen shows its charge on its faceplate and, with it picked, where the
power comes from, the charge, what it holds, the load, how long it lasts and when it shuts down; its header says ON
BATTERY or BYPASS.

A system knows its UPS. The battery stands in the notification area, green on the line, amber on the battery, red
near the end or in bypass, with its charge in the tip. As the line goes, Frames raises "On battery" with the charge,
how long it lasts and that it shuts down at 10%; Plasma raises Power Management's notice with its bar; the other
desktops their notice with the charge under it; MC-DOS and MC-NET print the UPS's two lines on the console, and a
Unix at its console gets the UPS daemon's broadcast. At 10% of the battery every system shuts itself down cleanly,
as real UPS software does: nothing is lost, no disk is checked, and the network's event log records a WARN. No
machine stays up on so low a battery; above it, a machine switched on in an outage starts on the battery.

### Cooling

*Added 2026-11-08.* Every computer turns its parts' watts into heat and has a **heat budget**, the watts it can
take away from itself: what its case takes away on its own, plus the cooler over each processor and its case fans
([design](design/temperature.md)).

| Case | Takes away | Fan seats (rear, top, front) |
| --- | ---: | --- |
| Vintage AT minitower | 60 W | 0, 0, 1 |
| Legacy ATX tower | 100 W | 1, 0, 1 |
| Transition mid tower | 150 W | 1, 0, 2 |
| Neutral (Standard, Advanced) | 200 W | 1, 0, 2 |
| High Performance (Standard, Advanced) | 300 W | 1, 2, 3 |
| Aesthetic (Standard, Advanced) | 150 W | 1, 1, 2 |

| Part | Vintage | Legacy | Transition | Standard | Advanced |
| --- | ---: | ---: | ---: | ---: | ---: |
| Stock cooler (a bare heat sink in the Vintage) | 20 W | 60 W | 100 W | 150 W | 200 W |
| Tower cooler | | | | 250 W | 300 W |
| MF AquaLoop 240 or 360 (all-in-one water cooler) | | | | 300 W | 400 W |
| Case fan (80, 80, 92, 120 and 120 mm) | 20 W | 30 W | 40 W | 50 W | 60 W |

They go in on the **COOLING** face of the assembly window, a tab under its lamps: a cooler seat over each processor
and the case's fan seats where the case has them; the tab's lamp lights while the computer lacks cooling. A cooler or
fan of the case's era or an older one fits. A water cooler takes its processor's cooler seat and two fan seats for
its radiator, the two on the top when the case has them, otherwise the two at the front (the 360 too), and brings its
radiator's own fans. Each part is drawn inside the case by its own model, the fans and coolers of an Aesthetic case
in its light. A Mainframe's cabinet takes away 1,500 W on its own, plus a cooler over each socket; a rack's servers
have none, their rack cools them.

The **temperature** is the surroundings' plus 60 °C times the heat over the budget, and it takes about half a minute
to get there. The surroundings come from the biome, 20 + 15 x (the biome's temperature - 0.8) °C, about 20 °C in the
plains, 38 °C in the desert and 8 °C in the snow, and the Nether adds 10 °C.

| Temperature | What happens |
| --- | --- |
| up to 80 °C | full speed |
| 80 to 90 °C | the speed drops a little, to 85% at 90 °C, as a processor that stops holding its boost |
| 90 °C | **throttling**: the computer slows just enough to hold 90 °C, its capacity and its programs' credits, down to 25% |
| 100 °C | **thermal trip**: when not even 25% holds the heat, it goes dark at once |

A computer slowed by heat wears its power supply twice as fast. A thermal trip is a sudden shutdown: the disks it had
in use are checked at the next start, the network's event log records an ERROR, and the computer does not start
again until it has cooled below 60 °C; its next self-test warns of the trip, and the boot goes on. The temperature
shows in the assembly window (TEMP, HEAT, BUDGET, AMBIENT and CLOCK), on the TEMP tile of the terminal's Local tab,
on the firmware's hardware page with the case fans that turn, and in the Device Manager under Cooling, beside the
UPS's battery under Batteries. A computer's heat is J's Core's thermal budget for any mod ([API](API.md)). The server
settings `temperature.ambient_base`, `biome_factor`, `nether_extra` and `restart_below_c` set the surroundings and
the restart; there is no switch to turn temperature off.

A rack has one temperature for its whole cabinet, by the same rules: its budget is 1,000 W, plus its Cooling Units,
plus 500 W for every **Vent Panel**, against the watts of its powered bays. Its throttling at 90 °C is the cut racks
always had, the budget over the load down to a quarter; at 100 °C every running bay goes dark at once, and none
switches on until the cabinet has cooled below 60 °C.

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

### Corruption and the Defrag

Storage is corrupted only by a network conflict, which corrupts a tenth of what both networks store, and by a thermal
trip, which corrupts each disk the computer had in use at a chance of 25%, 5% of it
(computers/docs/design/storage.md). Corrupted items leave the network's index and wait for the Defrag, still taking
their room; a system's own files can be hit too, and a system missing the file that starts it does not start. The
private part of a Personal Computer is never touched.

| Corrupted | What it does |
| --- | --- |
| Up to half | Only the items, waiting for the Defrag |
| Over half | Bad sectors take the capacity it goes over half by |
| Over nine tenths | The disk dies, with all it held; formatting does not bring it back |

The Defrag is DEFRAG on MC-DOS and MC-NET, the Disk Defragmenter on Frames 95, XP and 7, Optimize Drives on 10 and
11 (from the Start menu), and `fsck -y <device>` on Linux, FreeBSD and UNIX. A pass tries every corrupted item once
and mends a quarter of the bad sectors left; while it runs, that disk answers the network twice as slowly.

| Disk | Chance an item comes back | A pass takes |
| --- | --- | --- |
| Hard disk | 95% | 3 minutes |
| SSD | 70%, 10 less for every tenth corrupted; none from 70% | 1 minute |
| NVMe | 60%, 15 less for every tenth corrupted; none from 50% | 30 seconds |

### Cards

- **Sound cards** (the Artisan Tone Blaster family, Vintage to Transition): one a computer, of the board's era.
  From the Transition on, boards have sound of their own.
- **Crafting Cards**: what makes a Crafting Computer craft. Each drives a number of Crafting Interfaces and keeps
  that many crafting table recipes in its own memory: 2 in the Vintage, 4 in the Legacy, 5 in the Transition, 6
  in the Standard, 8 in the Advanced.
- **Workshop cards** (Crafting Table, Furnace, Enchanting, Anvil): let a Personal Computer's Workshop program
  craft, smelt, enchant and repair, and let the network change stored items the same way. Their work costs the
  computer energy (added 2026-11-08): smelting 2 kJ an item, enchanting 10 kJ for every level the enchanting table
  would take, repairing 2 kJ for every percent of durability given back, combining 10 kJ for every level the anvil
  would ask, naming 5 kJ. That is more than its store holds, so the computer owes it and pays it off as the grid
  fills the store; the cards' next paid work waits until it has. The server setting `update.cost` makes it levels
  instead (half of what the game's own block takes, at least one, and never lapis), both, or nothing. A player still
  needs the level an enchanting offer asks for.
- **Cluster Interface Cards** (Serial Console Card, Management NIC, Fabric Host Adapter): let a Cluster Management
  Computer reach racked machines; each newer one reaches more, and installs on several at once.
- **Optical Network Card**: lets a Mainframe or a Cluster Management Computer take fibre on the backbone (a rack
  takes it through a Fibre Optic Interface, and a server never seats the card).
- **Network Anchor Cards** (added 2026-11-08), one for each era's slot: while the computer runs, it keeps the chunks
  round it loaded through J's Core's chunk loading, under the name of whoever works the computer (the last player to
  place it or use it) and within their quota (`world.chunks_per_owner`, 25 unless the server says otherwise). The
  Vintage card (ISA, 5 W) and the Legacy one (PCI, 8 W) keep the computer's own chunk, the Transition (PCI-e 2.0,
  10 W) and the Standard (PCI-e 3.0, 12 W) the 3 by 3 round it, the Advanced (PCI-e 4.0, 15 W) the 5 by 5. A server
  in a rack takes one too, and the rack keeps the chunks round it for the farthest card of its running servers. A
  full quota leaves out the farthest chunks, and the network's log says so once; the card's tooltip shows how much of
  their quota its reader keeps already.
- **WAN Interface Cards** (added 2026-11-09), one for each era's long-distance line and only for a WAN Gateway
  Computer, four at most, one for each port on its back: the Modem Card (Vintage, ISA, Telephone Line, 1 it/t), the
  T1 Card (Legacy, PCI, Leased Line, 8 it/t), the T3 Card (Transition, PCI-e 2.0, T3 Line, 24 it/t), the VLDC Card
  (Standard, PCI-e 3.0, VLDC, 64 it/t) and the Optical Transport Card (Advanced, PCI-e 4.0, Dark Fibre, 256 it/t).
  Each takes the line of its era and the older ones. The n-th card in the gateway's slots works port n, the ports
  numbered 1 to 4 from the left seen from behind; a port's line is up while the gateway runs and a cable the card
  takes is plugged in, which the gateway's line modules and its screen's LINES lamps show.
- **Integra Phi** coprocessors (four of them), for supercomputer nodes.

## What can go wrong

- **The window refuses the build.** It names the reason: no CPU or no RAM; more of a part than the board has
  slots for; a CPU that does not fit the socket; processors of two instruction sets mixed; a card in the wrong kind
  of slot; memory of a generation the board does not take; two sound cards, or one of another era; more power
  drawn than the power supply gives.
- **The computer went dark and does not start, and its power button says "PSU failure".** Its power supply wore out
  and failed. Put in a new one; a supply that runs well under its rating lasts much longer.
- **The computer keeps going down on a red page that says INTERNAL_POWER_ERROR, or that the power is unstable.** The
  line gives it less than it draws: add generation, take machines off the line, or put a UPS between them.
- **The computers went dark when the power went, though a UPS stands beside them.** They draw more than the UPS
  carries, so it was in bypass. Use a bigger UPS, or fewer computers on it.
- **The computers shut themselves down while the power was out.** The UPS's battery fell to 10%. Bring the line
  back; for a rack, UPS Battery Expansions make it last longer.
- **A Drive Shelf's drives show on no server.** The shelf extends only the server mounted right above it.
- **Items an Operation was moving are gone, and the log says "Lost in transit".** The Mainframe went dark in the
  middle of it. Keep its power steady, or turn `item_loss.power_loss` off on the server.
- **A part sits in the window but does nothing.** The board does not offer that slot. Use a board with more.
- **A part does not go in at all.** It is from another era than the board.
- **The network is slow.** Look at the Mainframe first: its processors set how much it handles at once, and its
  graphics cards how many queues it works.

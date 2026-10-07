# Servers and racks

Servers, the racks they live in, the units that share a rack with them, datacenters, the supercomputer and the computer
that manages them all.

The pieces, from the smallest to the largest:

- **Server Case:** an item, the empty case of a chassis. It is an ingredient: the case a server will be made from once
  there are recipes.
- **Server:** an item, a whole computer with a height in rack units (U) and a node id of its own. It only works inside a
  rack, installs an operating system like any computer, and is assembled by using it in hand.
- **Server Rack:** a 2×3×2 block, 8U high, with 5 hot-swap front slots per U. It takes servers of its type and of its
  era or older, and rack units, and joins the backbone. The Supercomputer Rack is the same idea for another type of
  machine.
- **Datacenter:** not a block, but every Server Rack reached through one output face of a Server Router.
- **Cluster Management Computer:** a real computer that installs systems on the machines in the racks and watches over
  them.

## Four ideas behind it

1. **Servers stay independent.** A datacenter is a view over its servers, not a merge of them: every server keeps its
   own id and its own state.
2. **One foundation, different machines.** The Server, the Supercomputer Node and the AI Server share the same
   engineering: a headless chassis measured in U, assembled as an item, that only works inside a rack, installs a system
   and runs services. But they are different items, with their own hardware, their own work and their own type of rack.
   One piece of code serves all three without turning them into one machine in the game.
3. **Hardware is capacity, software is role.** A chassis decides how many bays, sockets and cards a server has. What a
   server *is* (a database, a mirror, a message server) comes from the services installed on its system
   ([Programs](programs.md)), never from a dedicated block or item. New roles come as software, from the mod or from
   add-ons, without new items.
4. **The monitor rule holds in the datacenter.** Software is always used through a monitor, or remotely (below). The
   rack's window is only physical: bays, hot-swap, power. No console, terminal or desktop is ever drawn in it.

## Servers

### Chassis and eras

Every server has a height in rack units and budgets of its own (estimates, to be tuned in play):

| Server | Era | Height | Drives | Gadgets | Processors | Cards |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| Vintage Server | Vintage | 1U | 1 | 0 | 1 | 1 |
| Legacy Server | Legacy | 1U | 2 | 1 | 2 | 1 |
| Transition Server | Transition | 1U | 4 | 1 | 2 | 2 |
| Server | Standard | 1U | 3 | 1 | 2 | the board's |
| Advanced Server | Advanced | 1U | 4 | 1 | 2 | the board's |
| Storage Server | Standard | 2U | 8 | 2 | 1 | 1 |
| Compute Server | Standard | 2U | 1 | 1 | 4 | the board's |
| Supercomputer Node | Standard | 2U | 1 | 1 | 2 | 2 |
| Advanced Supercomputer Node | Advanced | 2U | 2 | 1 | 2 | 4 |

"The board's" means as many as the motherboard offers, up to 6.

- The **Server** is the generalist: services and general server work.
- The **Storage Server** is where data is kept without worry: the most drives, 8 plus 2 gadgets. It has no performance
  bonus; its value is being bulky.
- The **Compute Server** is where heavy computing runs. Front slots beyond its budget are blocked by its cooling and its
  accelerators.

There are no hidden bonuses. The three rack computers differ like this:

| | Compute Server | Supercomputer Node | AI Server (to build) |
| --- | --- | --- | --- |
| Exclusive hardware | none: strong ordinary processors and graphics cards | Phi coprocessors | the GPU baseboard, compute cards and NPUs |
| Work | heavy services and general SubOperations | parallel crafting ([Autocrafting](autocrafting.md)) | training and generation ([Artificial intelligence](ai.md)) |
| Rack | Server Rack | Supercomputer Rack | AI Rack |

### Assembling a server

1. Hold the Server item and use it: the assembly window opens, the same as every other computer's.
2. The window has 1 EEB or EATX motherboard of the server's era; processors, memory and cards as the board offers and
   the chassis allows (the table above); and 1 power supply.
3. There are **no disk slots**. A server's disks live in the rack's hot-swap bays: adding or removing storage never
   means taking the server apart.
4. The hardware is kept on the Server item.

A server has no UPS slot: in a rack, the Rack UPS protects it from losing power ([Power](power.md)).

### A server in a rack

Outside a rack, a server does nothing: it is only the item with its hardware and its state. Inside one it is a full
computer:

- It gets its **node id** the first time it goes into a rack. The id stays on the item and moves with the server between
  racks. Every server is a full member of the network.
- It receives and runs Operations, shows up in the network's management, and **boots** the system installed on the disk
  in its bay: every bay is a machine with its own POST, boot manager, installer, desktop and windows, and the bay's
  switch is its power ([Operating systems](operating-systems.md)). A system's installer is kept on the item.
- It installs a real system on its bay disk: MC-NET, any Linux, FreeBSD, or Frames. There are two ways, by taste:
  whoever wants to be a sysadmin from the ground up uses Linux and ssh; whoever wants the easy way uses Frames and
  Remote Control. MC-NET stays cheap and immediate to install, so nobody is locked out.
- Installed services and programs stay on the disk, as on any computer ([Programs](programs.md)). The server services
  are the Predictive Cache, the Load Balancer, the Integrity Monitor, the Messenger Service, KnotHub and the
  Soundfoundry Server.
- A monitor reads it through the EEB board's console output or through its graphics cards' outputs
  ([Hardware](hardware.md)).
- Servers don't stack: each one is unique, with its own id.

## The Server Rack

A 2 wide × 3 tall × 2 deep multiblock (12 blocks), drawn as **one** model by its controller block. It holds servers and
rack units in bays measured in U, and it is the only place servers work. There is a Server Rack per era (Vintage,
Legacy, Transition, Standard, Advanced), and each takes servers of its era or older: a chassis of a later era is
refused.

- The rack carries the network without being a member of it: the servers inside are members, with their own ids, and the
  rack is only their physical container. It has no hardware of its own; every server has its own power supply,
  processors, memory and the rest.
- It is **8U** high. That is the block's shape, a constant, not a setting. Every chassis or unit takes its own height
  (1U or 2U), and the player arranges the rack freely within the 8U.
- **Front slots:** 5 hot-swap slots per U, which belong to the **rack** (they are positions on its front panel). A
  server that takes U 3 and 4 claims the slots of those rows, as its chassis allows; the rest are blocked, with the
  reason visible. Taking the server out **leaves** the drives where they are; the next chassis put in the same rows gets
  them.
- **Sound and light:** the fans spin while any bay is on; every server has its own noise, and the room its hum.
- **Heat:** a rack has a thermal budget its machines share ([Temperature](temperature.md)).

### Joining the network

- A rack only joins the **backbone** line of its era or older, never the Access or High compute lines. Backbone fibre
  (from the Standard on) only goes in if a server in the rack has an Optical Network Card
  ([Peripherals](peripherals.md)).
- The cable goes into the rack and reaches every server in it.
- Every server inside is a node of its own on the network, with its own id. The network's management lists them one by
  one; the rack is only a detail.
- A cable's speed is a limit **per Operation**, not a bandwidth that is shared out: every Operation goes at most at the
  speed of the slowest cable on its path ([The network](network.md)), and two Operations through the same cable each get
  its full speed. The limit is worked out when the network changes, never per tick.
- Empty slots don't affect the network.

### The rack's window

The window is only physical. A ruler of U on the left; every chassis takes its height; under each server's header, the
front slots of its rows.

```
+--------------------------------------------------------------+
| 1U | [KVM SWITCH · 1U]            1 monitor linked           |
+----+---------------------------------------------------------+
| 2U | [STORAGE SERVER · 2U] [PWR]   vault-01 · ONLINE · Debian|
| 3U |  [D][D][D][D][D]                                        |
|    |  [D][D][·][RAID][CACH]                                  |
+----+---------------------------------------------------------+
| 4U | [SERVER · 1U] [PWR]           web-01 · BOOTING · Ubuntu |
|    |  [D][D][·][CACH][x]                                     |
+----+---------------------------------------------------------+
| ...| (scrolls with the U)                                    |
+--------------------------------------------------------------+
|             the player's inventory and hotbar                |
+--------------------------------------------------------------+
```

- The number of **active** front slots follows the chassis in the bay. Slots beyond its budget show as **blocked**,
  always visible and always with the reason in the tooltip.
- The slots take drives (HDD, SSD, NVMe: [Storage devices](storage-devices.md)) and gadgets (the RAID Controller and the
  Cache Card, below). A gadget slot is generic: new gadgets go in without changing the model.
- **Real hot-swap:** drives can go in and out with the server **on**, with honest physical consequences, the same as on
  any computer: pulling the system's disk ends the session and that server's services at once; pulling a data disk takes
  that storage off the network at once. The network's index goes **stale**, and the index health view lists the item
  types out of place with the action to take (`reindex`, or `vacuum` for fragmentation, [Operations](operations.md)).
- A **POWER** button per bay turns that bay's server on and off. There is no console button in the rack's window: access
  is below.
- Shift-clicking a server takes it out of the rack. The drives stay in their rows (they belong to the rack); the next
  chassis gets them.

Using the rack with an empty hand opens the window. Using it while holding a server puts the server in the first space
of U that fits; the server gets its id if this is its first time in a rack. A chassis of the wrong type or of a later
era is refused, with a message in the action bar.

## Reaching a server

A server is headless by nature, and software is **always** used through a monitor, or remotely. Every way in leads to
the **same** stack (POST, boot manager, terminal or desktop), with no logic written twice:

1. **A monitor and a peripheral cable, on a rack with a single computer:** the monitor linked to the rack shows that
   server's screen, as if it were a desktop computer. No extra hardware is needed.
2. **A monitor and a KVM Switch, on a rack with two or more computers:** monitor access then **needs** a KVM Switch (a
   1U rack unit). The channel (the bay) is chosen when entering the monitor, with no bar on every screen; the active
   channel is kept, and keys go to it. It is the KVM of real life: one monitor, many machines, because there is a
   switch. Without one, a monitor has no way to address the bays.
3. **ssh:** from **any** terminal on the network (a text console, the Command Prompt, a terminal window on a desktop),
   `ssh <hostname>` opens a remote shell on the server (the shell of its system's family). `exit` goes back to the local
   shell. The remote prompt makes clear where it comes from, with the machine's name. Until the security system is
   built, being on the same network is enough to log in ([Security](security.md)).
4. **Remote Control** (an installable program, [Programs](programs.md)): the **easy** way. A graphical program that
   lists the network's rack machines and puts the chosen one's session (POST, firmware, terminal or the whole desktop)
   on this monitor. Whoever doesn't want a command line runs everything from here.

## Bay gadgets

### The RAID Controller

A RAID Controller in a bay's gadget slot joins the drives **of that bay** into an array. Its modes are set in the
controller's own window:

| Mode | Usable room | What it does in the game |
| --- | --- | --- |
| RAID 0 | the sum of the drives | Striping, 2 or more drives: +25% throughput for the bay's storage work. Losing or pulling **any** drive loses **everything** in the array (an honest risk). |
| RAID 1 | the smallest drive | A mirror, 2 or more drives: the array survives losing all but one drive. Putting one back starts an automatic **rebuild** (several ticks of work, a bar on the bay, lower performance while it runs). |
| RAID 5 | (n − 1) × the smallest drive | Parity, needs 3 or more drives: survives losing exactly one drive; rebuilds like RAID 1. |

- The array shows up on the network as **one** logical volume (one "disk" of the server).
- Formatting the array formats the whole logical volume.
- Pulling a member never duplicates items: the array is never read twice.
- Without a RAID Controller, a bay's drives are separate volumes, like any computer's disks.
- The numbers (+25%, rebuild times) are estimates.

A Storage Server with 8 drives and RAID 5 is the dream NAS; the player chooses between room, speed and resilience.

### The Cache Card

A read cache in the gadget slot: it cuts 25% off the effective latency of the reads served by that bay's drives (an
estimate). It adds up with the Predictive Cache service (−15%, [Programs](programs.md)), which is software and works on
top. It protects no data (the cache is volatile): turning the server off empties it.

## Rack units

Besides computers, a rack holds **units**: equipment measured in U that competes for the same room. The U is the rack's
currency: servers, storage, cooling, power, network and access all compete for the same 8U, and filling a rack is a real
puzzle of trade-offs.

| Unit | Height | What it does |
| --- | --- | --- |
| KVM Switch | 1U | Lets a monitor reach a rack with two or more computers. A rack with one computer doesn't need it. |
| Rack UPS | 1U | The UPS of the **whole** rack. |
| Cooling Unit | 1U | Active cooling: adds 1,500 W to the rack's thermal budget ([Temperature](temperature.md)). Dense racks of 2U Compute Servers need cooling or they throttle. |

## Racks by type

Racks share the same foundation (8U, 5 slots per U, units, the KVM, the physical window), with their own structures and
looks. Each takes **only** its type of computer: putting in the wrong item is refused with a clear message in the action
bar. Rack units go in all of them.

- **Server Rack** (one per era): Servers, Storage Servers, Compute Servers.
- **Supercomputer Rack** (Standard and Advanced): only Supercomputer Nodes. It only joins the High compute line (a
  Server Rack refuses High compute, and a Supercomputer Rack never hands a data network on).

## Datacenters and the Server Router

A datacenter is **not** a block in the world. It comes out of the topology: **every** Server Rack reached through
**one** output face of a Server Router, without going back, forms **one** datacenter, the **section** of that face.

The **Server Router** is a routing block that makes sections of the network out of its output faces. It is a topology
element: not a member of the network, and it receives no Operations. It has no motherboard and no processor of its own:
it is pure routing, like a layer 2 switch.

- Its **back** face (the one opposite its front) is the **input**: cabled to the backbone that goes to the Mainframe.
  Every Operation that enters the Server Router comes in through that face.
- The other **5** faces are **outputs**, and each defines a section. Output faces with no cable do nothing.

```
                       [Mainframe]
                            |
                     (backbone cable)
                            |
                       [input face]
                       /     |     \
              face A     face B     face C       (output faces)
                 |          |          |
            [Rack 1]    [Rack 4]   [Rack 6]
            [Rack 2]    [Rack 5]   [Rack 7]
            [Rack 3]               [Rack 8]
           ----------  ----------  ----------
           datacenter  datacenter  datacenter
               A           B           C
```

- Server Racks on the same output face of one Server Router form one datacenter; racks on different faces form different
  datacenters, which don't interact on their own.
- A Server Rack joined to the network without going through a Server Router is a standalone rack and forms no datacenter
  (its servers show up in the network's management as usual).
- Supercomputer Racks never go into a section.
- The player names each section.
- A Server Router takes 8 Server Racks in all, adding up every section. Racks over that are "unmanaged", with a warning.

The Server Router's window shows, for each active output face, the datacenter's name and its **balancing** mode, and how
many racks and servers each section has. Balancing decides where each write goes: ROUND_ROBIN keeps a rotation cursor
per face and splits a deposit into equal parts; LEAST_LOADED chooses by free room; MANUAL leaves it to the player. A
section works out its totals as it runs: storage, processor capacity, memory and throughput. Sections and their servers
are seen in the Cluster Manager (below).

When something fails:

- One server failing: the other servers in the same rack or datacenter carry on.
- One rack failing: its servers go offline but keep their state. The other racks in the section carry on; the datacenter
  only loses that share of its capacity.
- The Server Router failing: **all** its sections come apart as an idea. The racks are standalone until the router is
  replaced. No data is lost, only the grouping.

## The supercomputer

A supercomputer is the set of **Supercomputer Racks** (Standard and Advanced), with their nodes inside, joined to each
other by the **High compute** line (InfiniBand in the Transition, High Compute in the Standard, OSFP in the Advanced:
[The network](network.md)) and to **one** HBW Interface (Standard and Advanced), its uplink to the network.

- A **Supercomputer Node** (2U) has 1 drive, 1 gadget, 2 processors and 2 cards: a Phi and the graphics card the machine
  needs to be used. An **Advanced Supercomputer Node** (2U, with four sleds) has 2 drives, 1 gadget, 2 processors and 4
  cards.
- A Supercomputer Rack only answers to the High compute line; its link light means "the fibre reaches an HBW Interface
  that has a network".
- The HBW Interface registers on the network whenever its uplink has a network, with or without a Phi, so a cluster
  without coprocessors shows up in the network's management with the reason ("NO PHI CARD"). An Interface has six
  coprocessor slots ([Autocrafting](autocrafting.md)); each node takes one, in the order the fibre reaches it (an
  Advanced node counts as one), and a node too many is "unslotted".
- A node is INCOMPLETE, BAY OFF, INSTALLING, NO PHI, PHI LOW, INERT, NO SYSTEM or ONLINE.
- The Server Router leaves Supercomputer Racks out of its sections.

## The Cluster Management Computer

A real computer, in five eras (Vintage to Advanced) and, in the Standard and the Advanced, in the Personal Computer's
three cases. It has a Personal Computer's slots (1 processor, 4 memory modules, 4 cards, 2 disks, 18 storage slots) and
joins the network as a Personal Computer, on the Access line, never on High compute. It needs **one** card of the
**Cluster Interface Card** family (one per machine; without it, it is an ordinary computer), whose reach adds up:

| Card | Era | Slot | Reach | Lanes | Draw |
| --- | --- | --- | --- | ---: | ---: |
| Serial Console Card | Vintage | PCI | datacenters | 1 | 10 W |
| Management NIC | Legacy | PCIe 1.0 | and supercomputers | 2 | 20 W |
| Fabric Host Adapter | Standard | PCIe 3.0 | everything (the AI too) | 4 | 35 W |
| Fabric DPU | Advanced | PCIe 4.0 | everything (the AI too) | 8 | 75 W |

The lanes are cut by the slot's bandwidth ([Hardware](hardware.md)), never below 1. Clusters come from the **network**
(supercomputers and router sections), not from the fibre.

The **Cluster Manager** program has the Supercomputers, Datacenters and AI tabs, with nodes, a map and a queue; the
shell command `cluster list|nodes|power|install|status|cancel` does the same.

- Installing on many machines at once is a timed job per node (280 ticks at 2 GHz, between 60 and 1,200), in parallel up
  to the number of lanes, with reasons for skipping a node (bay off, already installed, no system, requirements, era).
- The media come from the drives linked to the manager; writing a system ends with the node at its POST.
- Each node is reached as a machine of its own, with the **same** install requirements as a Personal Computer: a
  shortcut, never a loophole.
- A cluster works without a manager; the manager only makes it one machine to drive.
- Sections and supercomputers ("SC-1") are renamed in the program.
- What the AI tab shows is designed with the AI ([Artificial intelligence](ai.md)).

## Saved state

Items keep their state in data components, never in raw NBT:

- **Server:** its hardware (motherboard, up to 4 processors, 8 memory modules, 6 cards, power supply), its node id, its
  system's state (history, installed programs, settings and windows, like any computer) and its name.
- **Disk:** its volume id and a summary of its use ([Storage devices](storage-devices.md)); in an array, the mode and
  its members.
- **Server Rack** (a block entity): what is mounted in each U, the front slots (5 per U, the rack's), the KVM's active
  channel and the names. Whether a slot is blocked is **worked out** from the chassis in its row, never saved.

## Models

The Server Racks (five eras), the Supercomputer Racks and the Mainframes (five eras) are each **one** GeckoLib model,
drawn by the controller block, with every other block of the multiblock invisible (rack 2×3×2, Mainframe 3×2×2).

- The rack shows every mounted unit at its U (counted from the top), and its fans spin while any bay is on.
- The Supercomputer Rack has a light bar and a service panel; its coolant inlet and outlet are on separate top blocks,
  for pipes to come.
- The Mainframe shows every processor, memory module, graphics card, disk and its power supply once they are installed,
  three lamps (running, network, fault) and a service panel per era. It has no operator panel: it is read on a linked
  monitor, like any computer.
- Modelling rules: no visible face shares a plane with another, the inside is a hollow shell, and items show the machine
  as it comes.

How racks are made is decided with how hardware is made ([Eras](eras.md)).

## To build

- **The Rack UPS** protecting the whole rack from a power loss, like a UPS ([Power](power.md)). Today it takes 1U with
  no effect.
- **More rack units**, each with its system:

  | Unit | Height | What it does |
  | --- | --- | --- |
  | UPS Battery Expansion | 1U | Makes the Rack UPS last longer. They stack. ([Power](power.md)) |
  | Vent Panel | 1U | Passive ventilation: takes away some heat, the cheap partner of the Cooling Unit; the filler that is not only for looks. |
  | PDU (Power Distribution Unit) | 1U | Measures FE per bay and makes the bays' power programmable: it exposes their power to IQL, so an UPDATE or a JOB turns servers on and off by rule. Real power automation. ([Power](power.md)) |
  | Drive Shelf (JBOD) | 2U | A shelf of drives only (10 front slots, no server): it extends the bays of the server **right above it** in the stack. With no server above, it does nothing. A Storage Server with shelves is the monster NAS of the late game. |
  | Tape Library | 2U | Cold storage for **data only**, for the Backup Service ([Programs](programs.md)): files, settings and system images on tapes. Huge, slow, and it **never** stores items, so nothing can be duplicated through it. |

- **The AI Rack** (Standard and Advanced), the third type of rack: only AI Servers and their units (the GPU Interconnect
  Switch, the Coolant Distribution Unit, the Power Shelf, the High Compute Switch), with a much larger thermal budget
  ([Artificial intelligence](ai.md)).
- **A Server Router per era**, like the ordinary routers, with the rack budget growing with the era (estimates): Legacy
  4, Transition 8, Standard 16, Advanced 32. The Vintage has no datacenter sections: its racks join the backbone
  directly. Today there is one Server Router, with 8 racks in all.
- **Diagnostics per section** (latency, throughput, capacity alerts), in the Cluster Manager, the program that runs the
  sections as one machine.

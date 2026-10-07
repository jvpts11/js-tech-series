# Computers

Every kind of computer in J's Computers, what each one is for, and how the Mainframe is backed up.

## What every computer has in common

- Every computer exists in every era with parts (Vintage, Legacy, Transition, Standard, Advanced), with the era in its
  name: Vintage Personal Computer, Legacy Mainframe. Its case only takes a motherboard of its own era ([Eras](eras.md)).
- A computer's block is its **case**. Using it opens the assembly window: the parts, turning it on and off, and its
  name, which is changed there and not on an anvil. Using it while crouching opens the case at the side and shows the
  parts installed, each with its own model. Everything else happens on a **monitor** linked to the computer
  ([Peripherals](peripherals.md)): the firmware, the system and the programs.
- To run, a computer needs a motherboard, a power supply, at least one processor and at least one memory module. To show
  an image, it needs a graphics card (or a processor with graphics on the die) and a monitor. To do anything useful, it
  needs a disk with an operating system ([Operating systems](operating-systems.md)).
- The motherboard offers slots, and the case counts at most its own ([Hardware](hardware.md)):

| Computer | Motherboards it takes | Processors | Memory | Cards | Disks | Storage slots |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Personal Computer | Baby-AT or AT (Vintage), ATX (Legacy), ATX or EATX (later) | 1 | 4 | 4 | 2 | 18 |
| Crafting Computer | Baby-AT or AT (Vintage), ATX (later) | 1 | 4 | 4 | 2 | |
| Cluster Management Computer | Baby-AT or AT (Vintage), ATX or EATX (later) | 1 | 4 | 4 | 2 | 18 |
| Mainframe | MTX | 4 | 8 | 6 | 4 | 27 |
| Server (in a rack) | EEB or EATX | 4 | 8 | 6 | the rack's bays | |

The **storage slots** are the computer's own shelf of items, where what it takes from the network arrives
([Storage](storage.md)). Most server chassis count fewer parts than the motherboard offers
([Servers and racks](servers-and-racks.md)).

## The computers

### Personal Computer

The player's own desk: their programs and their way into the network. It is where manual Operations come from. Its disks
are local storage, with a slider per disk for what is public and what is private ([Storage](storage.md)). It takes
monitors and peripherals through the peripheral cable, and joins the network on the Access line, through a router, never
on the backbone ([The network](network.md)). Its exclusive parts are the personal-use cards (the Workshop,
[Peripherals](peripherals.md)).

Its cases: a beige AT minitower (Vintage), an ATX tower (Legacy), a mid tower (Transition), and in the Standard and the
Advanced three cases, the neutral one, "High Performance" and "Aesthetic", which only differ in looks.

### Mainframe

The central orchestrator of the network. There is **exactly one** per network. It keeps the network's index in its
memory and runs the network engine ([Operations](operations.md)). Its processors give the network its capacity, and each
graphics card one more parallel queue ([Hardware](hardware.md)). Its disks hold its system, its engine and its programs,
not the network's storage. It joins the network on the backbone.

It is a 3×2×2 multiblock drawn as **one** model by its controller block, with running, network and fault lights and a
service panel that comes off. It has no operator panel: it is read on a monitor. It has a failover switch (below).

### Crafting Computer

It drives the network's crafts with its Crafting Cards: bench recipes live in the cards' memory, machine recipes in the
Crafting Interfaces ([Autocrafting](autocrafting.md)). Without a supercomputer, it runs one whole craft at a time, with
its steps in parallel up to the cards' threads. Its cases are the Personal Computer's.

### Server

A general-purpose **rack computer**: it installs a real system, runs services ([Programs](programs.md)) and gives the
network its public storage. There are three chassis (Server, Storage Server and Compute Server), and a Server per era.
The Server item keeps the hardware, without disks: disks live in the rack's bays. A server doesn't work outside a rack.
It is reached through a monitor, a KVM, ssh or Remote Control. Everything about servers and racks is in
[Servers and racks](servers-and-racks.md).

### Supercomputer

It lets many crafts run at once. It is the set of Supercomputer Racks (Standard and Advanced) with their Supercomputer
Nodes, joined by the High compute line to **one** HBW Interface. Its exclusive parts are the Phi coprocessors
([Servers and racks](servers-and-racks.md), [Autocrafting](autocrafting.md)).

### Cluster Management Computer

It installs systems on the machines in the racks and watches over them, through the Cluster Manager program. Without a
Cluster Interface Card it is an ordinary computer. Its cases are the Personal Computer's
([Servers and racks](servers-and-racks.md)).

## The Subframe

A Subframe is a whole computer, with the same hardware as a Mainframe: the same MTX motherboards of each era, the same
processors, graphics cards and memory those boards take. What sets it apart is only what it does.

What a Subframe gives the network:

1. It lends the orchestrating Mainframe part of its capacity: its capacity times the **Subframe factor** (0.6 by
   default).
2. Its graphics cards lend their parallel queues of Operations too, at their own speeds ([Hardware](hardware.md)).
3. It only works for a Mainframe running the **same** network engine as itself ([Operations](operations.md)): a Subframe
   with another engine lends neither capacity nor queues.
4. Without an orchestrating Mainframe, it sits idle and gives nothing.
5. The Services tab of the Network Manager lists the Subframes and marks the ones that get no work ("takes no work").

The network's total capacity:

```
total capacity = capacity of the Mainframe + Σ (capacity of each Subframe × factor)
```

The factor stands for the cost of coordinating the Mainframe and its Subframes. Cutting a Subframe's share on purpose
makes building more Subframes better than building one huge one. The factor is a key in `jstech-balance.toml`
(`balance.subframe_efficiency_factor`), kept within its range. There is no limit to how many Subframes a network has;
what each one costs is the limit.

A Mainframe always uses an MTX motherboard, and an MTX board has 4 processor sockets in every era: Socket 8 (Vintage),
Socket 940 (Legacy), Socket F (Transition), LGA 2011 (Standard), and SP3, SP5, LGA 3647 and LGA 4189 (Advanced).
Workstation processors (sTR4, sTR5, LGA 2066) don't go in a Mainframe: they belong to EATX boards. A Mainframe grows
sideways, through Subframes, not up, through bigger boards.

For example, a Mainframe and 3 identical Subframes, all on an MTX-A board (4 SP5 sockets) with all 4 sockets holding a
Velocion Epic 9654 (96 cores at 2.4 GHz, Way 4, with SMT):

```
one processor:      40 × 96 × 2.4 × 3.8 × 1.2 = 42,025 items per tick
the Mainframe:      4 × 42,025                 = 168,100
each Subframe:      168,100 × 0.6              = 100,860
three Subframes:    3 × 100,860                = 302,580
in total:           168,100 + 302,580          = 470,680 items per tick
```

A Subframe keeps no copy of the network's index while it is a Subframe.

The network already counts a Subframe's share; what doesn't exist yet is a way to make one (below).

## Failover

Every Mainframe has a **failover** switch in its assembly window. In failover, the Mainframe is **passive** on its
owner's network: it watches for an owner running on its stretch of the network. With no owner for 60 ticks (the count is
saved and survives a reload), the passive Mainframe with the lowest position takes over the **same** network and becomes
active. A Mainframe in failover never causes a conflict on its own.

- A passive Mainframe runs no dispatch and no Operations, and spends nothing waiting.
- The Operations of the owner that died are discarded; nothing resumes from a checkpoint.
- If the active Mainframe is destroyed and the passive one takes over, the network keeps working. The player can build a
  new passive Mainframe afterwards.
- Turning failover on in a Mainframe gives up the network it owned.

What happens when two Mainframes meet, and to a network whose Mainframe is gone, is in [The network](network.md).

## To build

### Making a Subframe

The role comes from the software. The network engine of a Mainframe ([Operations](operations.md)) runs in one of two
roles, **orchestrator** or **Subframe**, chosen in the Services tab of that machine's Network Manager or at the prompt
(`iqlengine role subframe`, and the same verb in the other engines). A Mainframe whose engine runs as a Subframe:

- doesn't count for network conflicts and doesn't start the network's index, which only the orchestrator has;
- lends capacity and queues to the orchestrator, if both run the same engine;
- goes back to orchestrating when the player changes the role; if the network already has an active orchestrator, that
  is a network conflict.

The failover switch stays as it is.

### A Subframe taking the orchestrator's place

When the orchestrator falls (no owner on the stretch for 60 ticks, as with failover), a Subframe takes the network over
as its Mainframe:

- a Mainframe in failover takes over first, if there is one; without one, a Subframe, first one running the same engine,
  then one running another engine, the one with the lowest position in each case;
- **the network's engine becomes the engine of the Subframe that took over**. A Subframe with another engine is thus a
  reserve: while the orchestrator runs, it lends nothing; when the orchestrator falls, the network carries on, with its
  engine;
- the other Subframes lend to the new orchestrator if they run its engine;
- what belongs to an engine (IQL's views and procedures, Prophet's states, Nextgre's rules) stays with the engine that
  had it, and the programs that need an engine say so ([Programs](programs.md));
- the fallen orchestrator's Operations follow the rule for a sudden shutdown ([Power](power.md)).

### Computers still to build

Each of these is designed with its own system, and like everything in J's Computers, each exists per era.

| Computer | What it is | Designed in |
| --- | --- | --- |
| PDA, Smartphone, Tablet | Portable devices with mobile systems, joined to the network over wireless: the PDA from the Legacy on, the Smartphone from the Transition on, the Tablet from the Standard on. | [Wireless](wireless.md) |
| AI Server | A server built for AI, with a GPU baseboard that pools its cards' video memory, in the AI Rack. | [Artificial intelligence](ai.md) |
| Mining Computer | The mining rig: a computer with no case, an open frame holding a motherboard and several graphics cards on risers, from the Transition on. Managing a fleet of rigs is software, not a computer. | [Teracoin](teracoin.md) |
| Security Control Computer | Not a block: the role of the server that runs the network's directory service. | [Security](security.md) |
| WAN Gateway Computer | A very wide computer that links distant places over the long-distance line, with WAN Interface Cards. | [The network](network.md) |
| Simulation Node | A node of the cosmological simulator's cluster, with the Singularity's hardware. | [The cosmological simulator](simulator.md) |

The Satellite Control Computer belongs to J's Space ([The series](series.md)).

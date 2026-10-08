# With J's Computers

What J's Industrial and J's Computers do together when both are installed.

Neither mod depends on the other: each depends only on J's Core ([The series](series.md)). Everything on this page wakes
up only when both are present, behind a guard, as any optional integration. Without J's Computers, every machine works
through its own screen, the Control Panel and redstone, and nothing of J's Industrial is missing.

## What exists today

Nothing: J's Computers' buses already move items and fluids through J's Core's capabilities, which any machine can
expose.

## To build

### Machines as boxes

Every machine works with J's Computers' buses with nothing special: crafting buses, the External Storage Bus, the Import
and Export buses all use J's Core's capabilities, which every J's Industrial machine exposes, for items and for every
state of matter (liquids, gases, plasmas and the rest, [Materials](materials.md)). To a Crafting Computer, a machine is
a box with inputs and outputs.

### The Industrial Controller Computer

To read a machine and command it, there is the **Industrial Controller Computer** (ICC), one per era, registered through
J's Computers' computer and hardware registries. It is the only computer that connects **industrial control cables** (8
blocks), through **Fieldbus Cards** of each era, each one adding machines it can reach (estimates):

| Era | Fieldbus Card | Machines |
| --- | --- | --- |
| Vintage | RS-485 / Modbus | 2 |
| Legacy | PROFIBUS | 4 |
| Transition | PROFINET | 6 |
| Standard | EtherNet/IP | 8 |
| Advanced | EtherCAT | 12 |

The number of cards is limited by the motherboard's slots; past that, another ICC.

- **The ICC reads** a machine's state (working, waiting, output full, no energy, undervoltage, blown fuse, damaged), the
  energy it draws and its port's class, its **temperature**, its progress, inputs and outputs, upgrades and battery; for
  reactors, also power, neutron flux, rods and **xenon** ([Nuclear](nuclear.md)).
- **The ICC changes** on and off, the redstone setting, and the **parameters each machine declares**: a reactor's target
  power and rods, a cascade's enrichment, a plant's pressure, a converter's direction. There is no fixed list.

### Machines that become computers

Some machines can **also be computers**: their controller gains a server assembly window (an EEB motherboard, CPU,
memory, power supply), boots a system, joins the network and runs its Operations and programs. **The hardware puts the
machine on the network; it is never what makes it run**: without it, the machine is still a machine, and installing J's
Computers in an existing world breaks nothing.

They are the machines that **make data**, or that in real life **need real-time computer control**:

| Family | Machines |
| --- | --- |
| Accelerators | Cyclotron, Heavy-Ion Cyclotron, Synchrotron, Linear Accelerator, Collider, LHC, SHE Factory |
| Instruments | Mass Spectrometer, ICP Mass Spectrometer, X-ray Diffractometer, Electron Microscope, NMR Spectrometer |
| Research | the CRC |
| Production | the Clean Room's controller (real fabs run on a manufacturing execution system), the Antimatter Factory |
| Mining | the Deep Core Miner, the Orion Miner |
| Fusion | the Tokamak and the Stellarator (real plasma only holds under real-time computer control) |
| Transcendence | the Wormhole Gate, the Aleph Gate, the Reality Projector |

- They run **their Operations**, sent by the Mainframe, and report their progress.
- They run **programs** (below).
- **Data becomes files:** the sample, particle and crystal data of a run are written to the network's storage instead of
  being items, and the CRC reads them from there, as in real science.
- They **gain no speed**: a better CPU doesn't speed an accelerator up.

Fission reactors are not on this list: they are controlled through the ICC.

**Research groups:** a **Research Router**, a topology element as the Server Router, groups CRCs on Research Link Cable:
each output face is a group, the back face joins the data network. The network sees each group as a cluster, a Cluster
Management Computer manages research groups in a **Research** tab of its Cluster Manager, and the Mainframe can send a
discovery to a group or split a big one among groups ([Science](science.md)).

### Operations

J's Industrial registers its Operation types in J's Core, and the Mainframe dispatches them as any other, with the eight
states of an Operation:

| Operation | Run by | What it does |
| --- | --- | --- |
| `MACHINE_READ`, `MACHINE_SET` | the ICC | reads and changes a machine's parameters |
| `VALVE_SET` | the ICC | opens, closes or throttles a valve |
| `BREAKER_SET` | the ICC | opens and closes a breaker of the grid |
| `STATION_SEND` | the ICC | a pneumatic station sends a capsule to an address; it waits without air |
| `ACCEL_RUN`, `ACCEL_ABORT`, `ACCEL_STATUS` | accelerators | a run: beam, target, energy, duration; it fails without its target and completes partially if the energy fails halfway |
| `ANALYZE` | instruments | analyses a sample and writes its data |
| `RESEARCH_START`, `RESEARCH_ABORT`, `RESEARCH_STATUS` | the CRC and its groups | a discovery; big ones are split into SubOperations among groups |
| `FAB_START`, `FAB_STATUS` | the Clean Room's controller | starts a lot with a product and follows it layer by layer; with the network, lots move by themselves between tools, and a busy tool locks the resource |
| `MINER_START`, `MINER_STOP`, `MINER_STATUS` | the Deep Core Miner, the Orion Miner | |
| `PLASMA_START`, `PLASMA_STOP` | the Tokamak, the Stellarator | starts and stops the plasma |
| `GATE_OPEN`, `GATE_CLOSE` | the Wormhole Gate, the Aleph Gate | opens to a destination and closes |
| `PROBE_LAUNCH` | the Aleph Gate | sends a probe into the Aleph and writes the data it brings back |
| `REPLICATE` | the Reality Projector | replicates N items from their record; it completes partially if the energy runs out first |

The ICC **exposes the state of the machines it controls as a table**: a `SELECT` can ask which machines have a blown
fuse, or which reactors are above 90% of their temperature, as any other query of the network.

### Programs

Installed on J's Computers' computers as any software, each era with its version. Their maker is a software house of the
series' own, as J's Computers' other programs have theirs.

| Program | What it does | In real life |
| --- | --- | --- |
| Reactor Control System | a reactor's state, target power, rods, **SCRAM**, a target temperature held by PID control, and **automation rules** ("IF the temperature passes X, THEN lower the power"); it runs on the ICC | the distributed control systems of power plants |
| Integrity Monitor | alerts from machines: blown fuses, damage, overheating, gas leaks, undervoltage | |
| Grid SCADA | the **map of the grid**: generators, lines, substations, breakers and meters, load and losses, with breakers commanded from afar; the Control Panel of [Energy](energy.md), at scale | grid SCADA systems, from the 1960s |
| Fab MES | the Clean Room's **lots**: where each is, its next layer, the **yield**, and routing between tools | the execution systems of chip fabs |
| Plant Historian | logs **any value of any machine over time**, with graphs | plant historians (OSIsoft's PI) |
| CAM Studio | writes **G-code**, the real language of CNC machines, for the NC Milling Machine and the CNC Machining Center; with J's Computers, it replaces punched tape | G-code was born in the 1950s and is still what is used |
| Slicer | prepares models for the 3D printers, resin and metal | |
| Mine Planner | turns what the prospecting pick and the scanner found into a **3D map of veins and reservoirs** | mine planning software |
| Lab Data Viewer | opens the **data files** of instruments and accelerators: spectra, diffraction patterns, particle tracks | |

### Everything else, together

- **The industrial path of hardware.** J's Computers on its own makes every part of every era by its simple path. With
  J's Industrial, every era is made with the lithography of its real time, boards on the SMT Line, disks on the Hard
  Disk and Optical Disc lines, the monitors of each era and the lasers of optical drives ([Clean Room](clean-room.md)).
  A server setting, off by default, makes the industrial path the only one.
- **Recipes are never hidden**: J's Computers' Pattern Studio and Crafting Manager show every recipe and mark the ones
  the team hasn't researched with the discovery they need ([Science](science.md)).
- The network commands **valves** and **breakers**, and **pneumatic stations**, which only push and never fetch or
  craft: asking, indexing and crafting stay J's Computers' ([Conveyors](conveyors.md)).
- The exo-suit's **HUD** shows the network's notifications ([Tools and armour](tools-and-armour.md)).
- A **probe** can go into the Aleph before a player ([The Aleph](aleph.md)).
- The **Reality Projector** takes its records from the network, and the **cosmological simulator** is a plentiful source
  of the exotic elements ([Exotic materials](exotic-materials.md)).
- **Computronium** is the matter of the Singularity era's hardware.
- The network's storage keeps every state of matter: items, liquids, gases, plasmas.

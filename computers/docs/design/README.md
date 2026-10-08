# J's Computers: the design

This folder contains the design spec of J's Computers. The entire mod is made based on these documents, with the sole
purpose of not only making every system that exists, and will exist, sane and easy to build by anyone, but also of
leaving no doubt about the things yet to be made.

Each page covers one subject. Inside a page, what the mod already does comes first; what is designed but not built yet
is in a section called **To build** at the end of the page, and the list at the bottom of this page gathers all of them
in one place. Every number in these pages is a first estimate, to be tuned in play.

For using the mod rather than building it, the player and developer guides are one folder up: the
[getting started guide](../GETTING_STARTED.md), [hardware](../HARDWARE.md), [the network](../NETWORK.md),
[systems and programs](../SYSTEMS.md), [Σ#](../SIGMA.md) and [the API](../API.md).

## The pages

| Page | What it covers |
| --- | --- |
| [Overview](overview.md) | What the mod is, its principles, the two ways to play it, and its configuration. |
| [Eras](eras.md) | The generations of computing, what fits with what, and the makers. |
| [Hardware](hardware.md) | Processors, memory, graphics cards and motherboards. |
| [Catalogue](catalogue.md) | Every part of every era with its figures, generated from the code. |
| [Storage devices](storage-devices.md) | Disks, removable media and volumes. |
| [Power](power.md) | Power supplies, energy, power supply failure, power loss and UPSes. |
| [Temperature](temperature.md) | Thermal budgets and what heat does. |
| [Computers](computers.md) | Every kind of computer, the Subframe and failover. |
| [Servers and racks](servers-and-racks.md) | Servers, racks and their units, datacenters, supercomputers. |
| [Peripherals](peripherals.md) | The peripheral cable, monitors, the printer, drives and expansion cards. |
| [Wireless](wireless.md) | Access points, Wi-Fi cards, portable devices and notifications. |
| [The network](network.md) | Topology, cable lines, routers, conflicts, distant networks, chunk loading. |
| [Operations](operations.md) | Everything the network does, its queues, priorities, logs and statistics. |
| [Storage](storage.md) | Local and network storage, buses, chemicals, corruption and recovery. |
| [Autocrafting](autocrafting.md) | How the network crafts on its own. |
| [Interface](interface.md) | The windows: the Network Interactor, the Network Manager and the rest. |
| [Programs](programs.md) | Programs, software houses, package managers, the Mirror and backups. |
| [Σ and Σ#](sigma.md) | The design of the mod's programming languages. |
| [Operating systems](operating-systems.md) | Firmware, kernels, systems, desktops, help, and the web. |
| [Security](security.md) | The directory, accounts, ID cards and hardware security modules. |
| [Artificial intelligence](ai.md) | Training models and generating items. |
| [Teracoin](teracoin.md) | The mod's cryptocurrency. |
| [The cosmological simulator](simulator.md) | The Singularity-era system, fixed in outline. |
| [Sound](sound.md) | What computers sound like, sound hardware and Soundfoundry. |
| [Manuals](manuals.md) | The in-game manuals. |
| [Advancements](advancements.md) | The mod's advancements. |
| [Formulas](formulas.md) | Every formula in one place. |
| [The API](api.md) | What add-ons can extend. |
| [Implementation](implementation.md) | Performance, configuration keys, compatibility and saved state. |
| [The series](series.md) | How J's Computers works with the other mods of the J's Tech Series. |

## To build

Everything below is designed and waiting to be built, in the order of the pages. Each item links to the page that
describes it.

- Recipes for every part, by J's Computers' simple path and J's Industrial's industrial path. [Eras](eras.md)
- Disks only in machines of their era or newer, and the Dock Station only taking disks of its era or older.
  [Eras](eras.md), [Peripherals](peripherals.md)
- The hardware era a player has reached, moved by building a working computer. [Eras](eras.md)
- Motherboards with expansion slots in groups, and PCI sound cards on Legacy boards. [Hardware](hardware.md)
- Real byte capacities for removable media. [Storage devices](storage-devices.md)
- Energy: computers draw J's Core's energy, in watts, through their power supply. [Power](power.md)
- Power supply wear, failure and the redundant 3000P. [Power](power.md)
- Sudden shutdowns, item loss when the network fails, disk checks at boot, UPS blocks and the Rack UPS.
  [Power](power.md)
- Thermal budgets and temperatures for every computer, with throttling and thermal trips. [Temperature](temperature.md)
- Making Subframes, and Subframes taking over a fallen orchestrator. [Computers](computers.md)
- The computers still to build: the portables, the AI Server, the Mining Computer, the WAN Gateway Computer and the
  Simulation Node. [Computers](computers.md)
- Rack units: UPS Battery Expansion, Vent Panel, PDU, Drive Shelf and Tape Library.
  [Servers and racks](servers-and-racks.md)
- The AI Rack. [Servers and racks](servers-and-racks.md), [Artificial intelligence](ai.md)
- A Server Router per era, and diagnostics per section. [Servers and racks](servers-and-racks.md)
- Wireless: access points, Wi-Fi cards, portable devices and their notifications. [Wireless](wireless.md)
- Network conflicts that lose items and corrupt both networks' storage. [The network](network.md)
- The Network Anchor Card and J's Core's Space Persistor. [The network](network.md)
- The WAN Gateway Computer: network extension and federation. [The network](network.md)
- DROP without asking, and the UPDATE's cost in energy. [Operations](operations.md)
- The network's event log. [Operations](operations.md), [Interface](interface.md)
- Storage corruption and the Defrag, and a storage key for every state of matter. [Storage](storage.md)
- In the interface: the draw and the id in the Local header, the performance graphs, the Craft Planner's Print and the
  CRAFT filter, and longer statistics. [Interface](interface.md)
- Programs: the "update available" badge, the Backup Service, the Media Player, the Data Compressor and the rest.
  [Programs](programs.md)
- Σ# generics and exceptions, two new event sources, and the language reference in the game. [Σ and Σ#](sigma.md)
- Accounts and passwords, the portables' mobile systems with a third family of systems, and the web of the world.
  [Operating systems](operating-systems.md)
- Security: the directory, roles, ID cards, hardware security modules, and later offensive security.
  [Security](security.md)
- The AI: AI Servers, datasets, models, training, generation and forecasting. [Artificial intelligence](ai.md)
- Teracoin: mining, pools, wallets, blocks and the 3D Printer. [Teracoin](teracoin.md)
- The cosmological simulator. [The cosmological simulator](simulator.md)
- The hardware, computer and era registries in the API, and dataset and model types. [The API](api.md)

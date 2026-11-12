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
| [Survival](survival.md) | The mod's own chain, its machines, the Prebuilder and the Preinstaller, abandoned buildings and villagers. |
| [Recipes](recipes.md) | What every block and item is made of, and where. |
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
| [Systems written by players](player-systems.md) | Operating systems and desktops players write in Σ or Σ#, and share. |
| [Quince](quince.md) | The third family: Quince's computers, servers, Mainframe and systems, after Apple's. |
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

- The lithography program on the computer linked to a Legacy or later Lithography Station, with its mask picker.
  [Survival](survival.md)
- Motherboards with expansion slots in groups, and PCI sound cards on Legacy boards. [Eras](eras.md),
  [Hardware](hardware.md)
- Compute cards without video outputs, and the two-socket boards drawn inside their case. [Hardware](hardware.md)
- Owners of files, with accounts. [Storage devices](storage-devices.md)
- Overvoltage on the computers, with their fuses; the bypass beep and the coil whine, with the sound pass.
  [Power](power.md)
- The AI Server, the Security Control Computer and the Simulation Node. [Computers](computers.md)
- The AI Rack, and the bay drives' own draw. [Servers and racks](servers-and-racks.md),
  [Artificial intelligence](ai.md)
- The NPUs, the HSM modules and the Simulation Interface. [Peripherals](peripherals.md)
- The portables' password and pattern, and Paint opening a PNG. [Wireless](wireless.md)
- Access control (GRANT, REVOKE and AUTH) and the audit log. [Operations](operations.md)
- Viruses and hacking. [Storage](storage.md)
- A graphics card's temperature. [Interface](interface.md)
- The firewall, the IDS and the antivirus, and the AI's programs. [Programs](programs.md)
- The player's generics, in version 4, and `System.Robots`. [Σ and Σ#](sigma.md)
- Accounts and passwords, and the web of the world. [Operating systems](operating-systems.md)
- Trust in the players' systems, with signatures. [Systems written by players](player-systems.md)
- Security: the directory, roles, ID cards, hardware security modules, and later offensive security.
  [Security](security.md)
- The AI: AI Servers, datasets, models, training, generation and forecasting. [Artificial intelligence](ai.md)
- Teracoin as J's Economy's currency. [Teracoin](teracoin.md)
- The cosmological simulator. [The cosmological simulator](simulator.md)
- The AI's dataset and model types, and Teracoin's API for J's Economy. [The API](api.md)
- The `[ai]` settings. [Implementation](implementation.md)

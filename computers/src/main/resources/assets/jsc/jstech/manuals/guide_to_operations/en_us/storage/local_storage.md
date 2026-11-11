---
section: storage
order: 119
icon: jsc:disk_vaultis_trench_20m
---
# Local storage

A disk does not carry its contents inside the item. The network keeps what the disks hold in one store for the whole save, and each disk item carries only a number that finds its part of that store. So what a disk holds travels with the disk: pull it from a computer and carry it to another machine, a rack or another dimension.

What a disk holds is counted in the same units as everything on the network: items, fluids and chemicals ([](jsc:items_as_data)). Once a disk holds something, its tooltip shows a line that starts with "Used".

Each computer keeps its own disks as local storage, and that storage works without any network. The computer shows its room in storage slots.

> [!DEFINE] Storage slot
> A unit the game uses to show a computer's room, 64 items each. It is not a limit on what a disk holds.

Table: Storage slots by computer

| Computer | Disk slots | Storage slots shown |
| --- | --- | --- |
| Personal Computer | 2 | up to 18 |
| Crafting Computer | 2 | up to 18 |
| Cluster Management Computer | 2 | up to 18 |
| Mainframe | 4 | up to 27 |

> [!WARNING] Formatting a disk erases everything on it: the items the network keeps there, and its files and installed systems. Take out what you need first.

Pulling a disk out of a computer does not erase it. The disk keeps its data for the next machine it goes into. In a server's bay, the array's RAID mode decides what happens to that data ([](jsc:raid_arrays)).

## Who may read a disk

The network does not read a computer's disks unless you let it. Each disk has a public share: the part of it that the network may read, counted in per-mille (parts of a thousand). A new disk is fully private, so its public share starts at 0 per-mille.

> [!DEFINE] Per-mille
> Parts of a thousand: 1,000 is the whole disk, 100 is a tenth of it.

Table: Public share of a disk

| Share, in per-mille | What the network does |
| --- | --- |
| `0` | Reads nothing from this disk. This is the default. |
| `500` | Reads half the disk. The other half stays private. |
| `1000` | Reads the whole disk. |

On a computer, you set the public share of its system disk in the Settings program, under Network, as "System disk public share" ([](jsc:this_pc_and_desktop_tools)). Each step moves it by 5%, which is 50 per-mille.

The network can take items out of a computer's public part, but it never puts items into it. The disks of a server are always fully public.

In the Network Manager ([](jsc:network_manager)), each computer's line shows its share as "Private". So "Private 90%" means a tenth of that disk is public.

## If something goes wrong

> [!PROBLEM] The Network Manager says "Private 100%" for your computer.
> Its system disk shares nothing. Raise the public share of the system disk in Settings, under Network.

> [!SEE] jsc:items_as_data, jsc:personal_computers, jsc:mainframes, jsc:disks

---
section: midsoft_installs
order: 197
icon: jsc:personal_computer
---
# Which Frames goes on which machine

Midsoft made five editions of Frames. Each edition is built for one era of computer, the generation a machine belongs to ([](jsc:eras)). A system runs on the era it was made for and on every newer era, but never on an older machine.

Choose the edition by the machine you have. The editions also have a rank, from 1 for Frames 95 to 5 for Frames 11. Programs use the rank when they ask for a Frames edition, such as XP or newer. The installer does not use it.

The install medium is the compact disc (CD), digital versatile disc (DVD) or Universal Serial Bus (USB) stick that a system is installed from. You read a medium in a drive joined to the computer: a CD goes in a CD Drive, a DVD in a DVD Drive, and a USB stick in a Dock Station ([](jsc:dock_station)).

Table: The five editions and their machines

| Edition | Machine era | Install medium |
| --- | --- | --- |
| Frames 95 | Legacy | CD |
| Frames XP | Legacy | CD |
| Frames 7 | Transition | DVD |
| Frames 10 | Standard | USB stick |
| Frames 11 | Advanced | USB stick |

Own RAM is the random-access memory (RAM) that the running system keeps for itself before any program opens. Footprint is the space the system takes on its disk. Both are counted in megabytes (MB), and the table below gives them for each edition ([](jsc:frames_memory)).

```table
{"caption": "Own RAM and footprint of each edition", "heads": ["Edition", "Own RAM (MB)", "Footprint (MB)"], "rows": [[{"words": "Frames 95"}, {"amount": 32, "unit": "MB"}, {"amount": 48, "unit": "MB"}], [{"words": "Frames XP"}, {"amount": 128, "unit": "MB"}, {"amount": 1536, "unit": "MB"}], [{"words": "Frames 7"}, {"amount": 1024, "unit": "MB"}, {"amount": 16384, "unit": "MB"}], [{"words": "Frames 10"}, {"amount": 3072, "unit": "MB"}, {"amount": 16384, "unit": "MB"}], [{"words": "Frames 11"}, {"amount": 8192, "unit": "MB"}, {"amount": 20480, "unit": "MB"}]]}
```

A disk measures its room in items, and an item takes 1 MB on a Vintage disk, 16 MB on a Legacy disk and 256 MB on a disk of the Transition era or newer. So the same system takes fewer items on a newer disk. The footprint counts whole items, rounded up ([](jsc:disks)).

Table: Items each edition takes on a disk

| Edition | On a Legacy disk | On a Transition, Standard or Advanced disk |
| --- | --- | --- |
| Frames 95 | 3 items | 1 item |
| Frames XP | 96 items | 6 items |
| Frames 7 | 1,024 items | 64 items |
| Frames 10 | 1,024 items | 64 items |
| Frames 11 | 1,280 items | 80 items |

A Legacy disk of 4 gigabytes (GB), such as the Vaultis Link IDE-HDD 4G, holds 4,096 MB. That is room for Frames XP, which needs 1,536 MB, but not for Frames 7, which needs 16,384 MB. The disks you can buy are listed in [Disks](jsc:disks).

The era also sets the firmware, the program that starts the machine. Frames 95, XP and 7 start in the blue basic input/output system (BIOS), and Frames 10 and 11 in the Unified Extensible Firmware Interface (UEFI) ([](jsc:firmware)).

The memory of a machine comes from its memory modules. The smallest module of the Legacy era is the Stratix Layer SDRAM-128, with 128 MB. A Legacy machine with that module runs Frames XP without tight RAM, which the memory page explains ([](jsc:frames_memory)).

> [!DEFINE] Edition
> One version of Frames, with its own installer and its own desktop.

> [!DEFINE] Install medium
> The disc or stick a system is installed from, such as a CD or a USB stick.

> [!DEFINE] Firmware
> The program on the motherboard that starts the computer and tests its hardware before it hands over to a system.

> [!DEFINE] Own RAM
> The memory the running system keeps for itself before any program opens, in megabytes.

> [!DEFINE] Footprint
> The space a system takes on its disk, in megabytes. The disk counts it in items.

## If something goes wrong

> [!PROBLEM] The screen says: "Frames 10 needs Standard era hardware or newer; this machine is Legacy era."
> Choose the edition for this machine's era, or use a newer machine ([](jsc:eras)).

> [!PROBLEM] The screen says: "No disk is installed to put Frames 7 on."
> Install a disk in the machine first, then start the installer again ([](jsc:disks)).

> [!PROBLEM] The screen says: "The target disk has no room for", then the system's name and, in brackets, the megabytes it needs.
> Choose a bigger disk, or another disk with room ([](jsc:disks)).

> [!SEE] jsc:frames_setup_95_xp, jsc:frames_setup_7_10_11, jsc:eras

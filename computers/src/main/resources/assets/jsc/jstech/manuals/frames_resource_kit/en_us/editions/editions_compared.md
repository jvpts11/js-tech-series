---
section: editions
order: 227
---
# The editions side by side

Frames is one family of five editions. An edition is one version of Frames, with its own installer, its own desktop and its own built-in programs. The editions have an order, from Frames 95, the oldest, to Frames 11, the newest.

Each edition has a family rank: its place in the family's order, counted from one. A program can ask for a version by its rank. A program that needs "Frames XP or newer" runs on XP, 7, 10 and 11, and it refuses Frames 95.

Every edition needs a computer of its own era or newer. Frames 95 and XP need Legacy, Frames 7 needs Transition, Frames 10 needs Standard and Frames 11 needs Advanced. Each edition also runs on every computer of a newer era. A newer edition does not go on an older computer, and the installer tells you so ([](jsc:the_systems)).

The install medium is the item you put in a drive to install an edition. Frames 95 and XP install from a compact disc (CD), Frames 7 from a digital versatile disc (DVD), and Frames 10 and 11 from a universal serial bus (USB) stick, which goes in the [Dock Station](jsc:dock_station).

Table: The five editions

| Edition | Family rank | Minimum era | Install medium |
| --- | --- | --- | --- |
| Frames 95 | 1 | Legacy | CD, in the CD Drive |
| Frames XP | 2 | Legacy | CD, in the CD Drive |
| Frames 7 | 3 | Transition | DVD, in the DVD Drive |
| Frames 10 | 4 | Standard | USB stick, in the Dock Station |
| Frames 11 | 5 | Advanced | USB stick, in the Dock Station |

```table
{"caption": "What each edition takes", "heads": ["Edition", "Footprint", "Random-access memory (RAM) held", "Items on its era's disk"], "rows": [[{"words": "Frames 95"}, {"amount": 48, "unit": "MB"}, {"amount": 32, "unit": "MB"}, {"words": "3 items on a Legacy disk"}], [{"words": "Frames XP"}, {"amount": 1536, "unit": "MB"}, {"amount": 128, "unit": "MB"}, {"words": "96 items on a Legacy disk"}], [{"words": "Frames 7"}, {"amount": 16384, "unit": "MB"}, {"amount": 1024, "unit": "MB"}, {"words": "64 items on a Transition disk"}], [{"words": "Frames 10"}, {"amount": 16384, "unit": "MB"}, {"amount": 3072, "unit": "MB"}, {"words": "64 items on a Standard disk"}], [{"words": "Frames 11"}, {"amount": 20480, "unit": "MB"}, {"amount": 8192, "unit": "MB"}, {"words": "80 items on an Advanced disk"}]]}
```

The footprint is the room a system takes on a disk once it is installed. A disk counts its room in items, and an item is 1 MB on a Vintage disk, 16 MB on a Legacy disk and 256 MB from the Transition on. So Frames 95 takes 3 items of a Legacy disk.

The RAM held column is the memory that each system keeps for itself before any program opens.

> [!DEFINE] Era
> A generation of hardware, such as Legacy or Standard.

> [!DEFINE] Family rank
> An edition's place in its family's order, counted from one.

> [!DEFINE] Footprint
> The room a system takes on a disk once it is installed, counted in the disk's items.

## If something goes wrong

> [!PROBLEM] The installer says "Frames 10 needs Standard era hardware or newer; this machine is Legacy era."
> The computer is older than the edition. Use a computer of the era the message names, or install an older edition ([](jsc:installing)).

> [!PROBLEM] The installer says the target disk has no room for the edition, and gives the megabytes it needs.
> Use a larger disk. A Legacy disk holds Frames 95 in 3 items, and Frames 11 needs 80 items of an Advanced disk.

> [!PROBLEM] The installer says "No disk is installed to put Frames 11 on."
> Install a disk in the computer first ([](jsc:disks)).

> [!PROBLEM] The installer says "The system on the medium is not known to this machine."
> Each install medium belongs to one system. Use the medium made for the edition you want ([](jsc:installing)).

> [!SEE] jsc:installing, jsc:eras, jsc:midsoft_systems

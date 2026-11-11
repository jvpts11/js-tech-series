---
section: starting_a_computer
order: 5
icon: jsc:motherboard_atx_standard_lga1150
---
# The Setup

The Setup is the firmware's own settings screen. You open it with [[DEL]] while the self-test runs ([](jsc:turning_on)). The title of the screen ends with BIOS Setup Utility.

The Setup has pages. Press [[Tab]] to move to the next one.

Table: The pages of the Setup

| Page | What it shows |
| --- | --- |
| `Boot` | The entries you can start from. Enter starts the selected one. |
| `Boot Order` | The disks in the order the firmware tries them. Enter on a disk sets it as first boot device. |
| `Hardware` | The parts the firmware found in this machine. |
| `Storage` | The RAID modes. Only a machine with a storage controller has this page. |

The Hardware page lists Processor, Instruction Set, Cores, Memory, Graphics, Video, Board, Monitors, Hardware Era, Boot Disk and Install Target. The Memory line shows the size in megabytes (MB) and how many of the slots hold a module.

Table: The keys in the Setup

| Key | What it does |
| --- | --- |
| `Tab` | Goes to the next page. |
| `Up and Down` | Move the selection. On the Storage page they change the RAID mode. |
| `Enter` | Activates the selected row: starts an entry, sets a first boot device, or applies a RAID mode. |
| `F` | Formats the selected disk. The first press only arms it, and the second press formats. |
| `1 to 9` | Selects that row. |
| `Escape` | Leaves the Setup. |

> [!WARNING] Formatting a disk erases everything on it, and the data cannot be brought back.

The Storage page shows ARRAY MODE with four options for the RAID array ([](jsc:raid_arrays)). Each one needs drives. Enter applies the mode, and changing it erases the array.

Table: The RAID modes on the Storage page

| Option | Note on the screen |
| --- | --- |
| `NONE (independent)` | None: each drive works on its own. |
| `RAID 0` | \+25% throughput |
| `RAID 1` | survives to 1 drive |
| `RAID 5` | survives 1 loss |

The Exit row leaves the Setup.

## If something goes wrong

> [!PROBLEM] The Storage page says "No storage controller fitted."
> This machine has no storage controller, so it has no RAID modes ([](jsc:raid_arrays)).

> [!SEE] jsc:firmware, jsc:boot_menu, jsc:installing

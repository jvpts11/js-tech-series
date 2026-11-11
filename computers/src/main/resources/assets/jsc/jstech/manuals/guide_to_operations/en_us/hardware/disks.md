---
section: hardware
order: 22
icon: jsc:disk_vaultis_trench_20m
covers_family: jsc:disks
---
# Disks

A disk keeps what must survive turning the computer off: its system, its programs and your files. The size of a disk is counted in items, as [Items as data](jsc:items_as_data) explains.

There are three kinds of disk, and they differ in speed. The hard disk is the slowest, the SSD is four times as fast, and the NVMe is sixteen times as fast.

> [!DEFINE] Hard disk (HDD)
> The slowest kind of disk here.

> [!DEFINE] Solid-state drive (SSD)
> A disk four times as fast as a hard disk.

> [!DEFINE] NVMe
> A fast storage standard, short for Non-Volatile Memory Express. It is the quickest kind of disk here.

> [!DEFINE] Latency
> The ticks a disk waits before it starts a transfer. The table gives the defaults.

```table
{"caption": "The kinds of disk", "heads": ["Kind", "Speed", "Wait before a transfer (default)", "Power"], "rows": [[{"words": "Hard disk (HDD)"}, {"literal": "x1"}, {"words": "10 ticks"}, {"amount": 6, "unit": "W"}], [{"words": "Solid-state drive (SSD)"}, {"literal": "x4"}, {"words": "3 ticks"}, {"amount": 3, "unit": "W"}], [{"words": "NVMe"}, {"literal": "x16"}, {"words": "1 tick"}, {"amount": 5, "unit": "W"}]]}
```

A disk's size is counted in items of its era. A 20 MB Vintage drive holds 20 items. A 1 TB disk of a later era holds 4,096 items ([Units and ticks](jsc:units_and_ticks)).

Table: Disk sizes

| Size | Items | Kinds it comes in |
| --- | --- | --- |
| 500 GB | 2,000 items | Hard disk, SSD and NVMe |
| 1 TB | 4,096 items | Hard disk, SSD and NVMe |
| 2 TB | 8,192 items | Hard disk, SSD and NVMe |
| 4 TB | 16,384 items | Hard disk, SSD and NVMe |
| 8 TB | 32,768 items | Hard disk, SSD and NVMe |
| 12 TB | 49,152 items | Hard disk only |
| 16 TB | 65,536 items | Hard disk only |
| 20 TB | 81,920 items | Hard disk only |
| 24 TB | 98,304 items | Hard disk only |

The Vintage disks are Vaultis Trench HDD drives of 20M, 100M and 200M. The Legacy hard disks are Vaultis Link IDE-HDD drives of 4G, 20G and 40G, and the Transition SSD is a Vaultis Link SATA-SSD of 64G.

A disk fits a computer of its own era or of a later one. A disk newer than the computer is refused, in a disk slot, in the hot-swap bay and in a rack's drive bay, and the slot says which era the disk needs when you hold it over the slot. The Dock Station ([](jsc:dock_station)) takes a disk of the Standard or of an older era.

The board's disk slots count the disks you put in them. A disk in the hot-swap bay takes none of those slots. Each disk also draws power, as the table shows.

## If something goes wrong

> [!PROBLEM] The disk does not go into the disk slot.
> It is newer than the computer. Use a disk of the computer's era or of an older one.

> [!PROBLEM] The build says there are too many disks.
> Move disks to the Hot-Swap Drive Bay, or use a board with more disk slots ([the build refusals](jsc:build_refusals)).

> [!SEE] jsc:case_front, jsc:items_as_data, jsc:the_systems, jsc:partitions

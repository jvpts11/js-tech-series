---
section: how_things_are_counted
order: 13
icon: jsc:disk_vaultis_trench_20m
---
# Items as data

The network keeps your items on the disks of its computers, as data. Its disks count items, not raw space. A bucket of fluid takes as much room as one item.

How much room an item takes depends on the era of the disk, because older computers had smaller words: 1 MB in the Vintage, 16 MB in the Legacy, and 256 MB from the Transition on. So a 20 MB Vintage drive holds 20 items, and a 1 terabyte (TB) disk holds 4,096 items from the Transition on.

> [!DEFINE] Item size
> The room an item takes on a disk: 1 MB in the Vintage, 16 MB in the Legacy, 256 MB from the Transition on.

Table: Disk sizes in items, from the Transition on

| Disk | Items |
| --- | --- |
| 500 GB | 2,000 items |
| 1 TB | 4,096 items |
| 2 TB | 8,192 items |
| 4 TB | 16,384 items |
| 8 TB | 32,768 items |

Above 8 TB, only hard disks are made, up to 24 TB, which holds 98,304 items.

Memory works in items too, in its own way. Each memory module keeps a buffer of items for the Mainframe, one item for every 4 MB of the module's size ([](jsc:memory)). A buffer too small for the processor leaves the processor waiting for memory.

Disks differ in speed. A solid-state drive (SSD) is 4 times as fast as a hard disk, and an NVMe disk is 16 times as fast ([](jsc:disks)).

> [!SEE] jsc:disks, jsc:units_and_ticks, jsc:fluids

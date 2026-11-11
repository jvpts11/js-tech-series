---
section: how_things_are_counted
order: 14
---
# Units and ticks

Several units appear in this manual and on the screens. This page explains each one, so the numbers mean something to you.

Table: The units of this book

| Unit | What it measures |
| --- | --- |
| `item` | One unit of everything the network keeps. Disk room is counted in items. |
| `MB, GB, TB` | The real size of a disk or a memory module, in megabytes, gigabytes and terabytes. |
| `it/t` | Items a tick: a processor's tooltip shows it. |
| `W` | Watts: how much power a power supply gives, and how much a part draws. |
| `s` | Seconds. The boot manager counts down in them. |

> [!DEFINE] Tick
> One of the game's beats. 20 ticks make a second, so a tick is one twentieth of a second.

> [!DEFINE] Items a tick (it/t)
> How many items a processor handles in one tick. A faster processor handles more items each tick.

The game counts time in ticks, too. The boot manager waits 5 seconds, which is 100 ticks ([](jsc:boot_menu)).

Memory is counted in items as well. Each memory module's buffer holds one item for every 4 MB of its size, and the disks count by the era of each disk ([](jsc:items_as_data)).

> [!SEE] jsc:items_as_data, jsc:boot_menu

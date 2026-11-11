---
section: storage
order: 123
icon: jsc:vaultis_array_pcie3
---
# When storage goes wrong

Storage goes wrong in a few known ways, and the game says which. An [Operation](jsc:what_operations_are) that does not finish its whole job shows its status in the Network Manager ([](jsc:network_manager)): "partial" when some of the job was done, "failed" when none was, and "locked" when it gave up waiting.

Each Operation reserves the items it will take, and another Operation that needs the same items waits for it. In the messages below, <item> stands for the name of the item.

## Operations on the network

Table: Messages of an Operation

| Message | What it means, and the fix |
| --- | --- |
| "the network had no room left for the <item>" | Not all of the items fit. Add disks or servers to the network. |
| "the network was not holding enough <item>" | The network had less of the item than you asked for. Stock more of it, or ask for less. |
| "another Operation was holding the <item> it needed" | Another Operation reserved the items this one needs. Wait for it, or cancel it ([](jsc:cancelling_and_the_log)). |

## Buses and inventories

Table: Messages at a bus and an inventory

| Message | What it means, and the fix |
| --- | --- |
| "the chest is full" | The inventory has no free room. Take items out of it. |
| "on no network" | The bus's cable is not on a network. Join it to one ([](jsc:data_cables)). |
| "no inventory before it" | The bus faces nothing. Put it on a data cable beside an inventory. |
| ON DEMAND, with "Moves only while its cable has a redstone signal" | The bus moves only when its cable has a signal. Give the cable a redstone signal, or change the bus's mode. |

## If something goes wrong

> [!PROBLEM] The bus moves nothing.
> Its filter lets nothing through, its mode is ON DEMAND with no redstone signal on its cable, or it is on no network. Check each of these.

## Disks and racks

Table: Messages about disks and RAID

| Message | What it means, and the fix |
| --- | --- |
| "<mode> - needs <number>+ drives" | The bay has fewer disks than that RAID mode needs, so the change is refused. Add disks, then set the mode again. |
| "<name> DEGRADED" | The array lost disks but still serves its items. Put a replacement disk in the bay ([](jsc:raid_arrays)). |
| "<name> FAILED" | More disks are gone than the mode can lose, so the array gives no room. Put the missing disks back. |
| "REBUILD <number>%" | A replacement disk is copying the array's items back. The array serves again when the copy is done. |
| "Blocked - the chassis does not cable this slot" | That slot cannot hold a disk. Use another slot. |

> [!WARNING] Pulling any disk out of a RAID 0 array erases the data on every disk of the array. RAID 0 keeps no second copy.

Pulling a disk out of a RAID 1 or RAID 5 array keeps its items on the other disks. The array keeps working while it waits for a replacement, and it rebuilds when the replacement goes into the bay ([](jsc:raid_arrays)).

## Room on a disk

## If something goes wrong

> [!PROBLEM] An Operation says "the network had no room left for the <item>".
> Add disks or servers. A bigger disk holds more items. A newer era's disk holds more megabytes.

> [!PROBLEM] A file does not fit, and the computer says "Not enough free space".
> Delete files you do not need, or use a bigger disk.

> [!PROBLEM] Installing a system says "The target disk has no room for <system> (<number> MB needed)."
> Use a bigger disk. The number is the room in megabytes (MB) that the system needs.

> [!SEE] jsc:local_storage, jsc:external_storage_bus, jsc:raid_arrays, jsc:disks

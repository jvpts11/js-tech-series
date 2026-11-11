---
section: racks
order: 57
icon: jsc:vaultis_array_pcie3
covers_family: jsc:raid_arrays
---
# RAID modes

A RAID controller is an expansion card: the Vaultis Array of each era, in a server's expansion slot. It joins the server's drives into one array, and until a mode is set, the drives stay separate. You set the array mode in the server's firmware, on the storage page of the Setup ([](jsc:firmware)). The card keeps the mode, so it goes with the card.

```table
source: jsc:storage_cards
caption: Storage cards
heads: Card | What it is | Slot | Era | Read latency cut
```

Table: Array modes

| Mode | Drives needed | Survives a lost drive | Throughput |
| --- | --- | --- | --- |
| NONE | 1 | No | 100% |
| RAID 0 | 2 | No | 125% |
| RAID 1 | 2 | Yes, down to one drive | 100% |
| RAID 5 | 3 | Yes, one drive | 100% |

The usable space depends on the mode. RAID 0 uses the space of all its drives. RAID 1 gives the space of its smallest drive. RAID 5 gives the space of its drives but one, at the size of the smallest. RAID 0 never rebuilds, since it keeps no copy. The screen shows "+25% throughput" for RAID 0, and "survives to 1 drive" for RAID 1.

1. Open the Setup of the server and go to its storage page.
2. Press Up or Down until the mode you want shows under ARRAY MODE.
3. Press [[Enter]] to apply it.

The read cache, the Vaultis Cache of each era, needs no mode. In a server's expansion slot it cuts the read latency of the server's drives, by more in each newer era, as the table shows. Outside a server, a storage card sits in its slot and does nothing.

## If something goes wrong

> [!PROBLEM] The server's drives do not join.
> No mode is set, so the drives stay independent. Set a mode on the storage page.

> [!PROBLEM] The firmware says "No storage controller fitted."
> Seat a Vaultis Array card in one of this machine's expansion slots.

> [!PROBLEM] The firmware says the mode needs more drives, such as "needs 3 drives".
> Add drives to the server's bays, or choose a mode that needs fewer.

> [!SEE] jsc:rack_drive_bays, jsc:firmware

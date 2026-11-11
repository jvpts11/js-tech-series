---
section: racks
order: 56
---
# Drive bays and hot-swap

A drive bay holds one disk. You can pull the disk out of its bay and put it back while the machine is on, which is called hot-swap.

> [!DEFINE] Hot-swap
> Taking a part out of a machine, or putting one in, while the machine stays on.

The bays belong to the rack, not to the server. A drive stays in its bay when the server is pulled out, and the next server mounted over those rows inherits its drives.

A drive bay takes only a disk of the rack's era or an older one, as a computer's disk slots do ([](jsc:disks)).

Drives can join into one volume, called RAID. Depending on the mode, a RAID array keeps working when a drive is lost ([](jsc:raid_arrays)).

> [!DEFINE] RAID
> A way of joining several drives into one volume, with or without a copy of the data.

What happens when you pull a drive depends on the bay it is in.

Table: Pulling a drive

| Bay | What happens |
| --- | --- |
| An independent drive | It leaves with the items on it. |
| A RAID 0 array | The whole array loses its items. |
| A RAID 1 or RAID 5 array | The array keeps working while a replacement drive rebuilds it. |

In a RAID 1 or RAID 5 array, a pulled drive's contents move to the drives that stay, as far as those drives have room for them.

A rebuild takes 20 ticks for each 1,000 items of array capacity. It never takes less than 40 ticks, which is 2 seconds, or more than 3 minutes. While it runs, the rack shows REBUILD and a percentage. The rack also marks an array DEGRADED or FAILED.

## If something goes wrong

> [!PROBLEM] The bay refuses the item.
> A drive bay takes only a disk of the rack's era or an older one. Hold the item over the bay to read which rule keeps it out.

> [!PROBLEM] The array lost everything.
> A RAID 0 array keeps no copy, so it loses everything when a drive is lost. Use RAID 1 or RAID 5 for a copy ([](jsc:raid_arrays)).

> [!SEE] jsc:server_types, jsc:disks

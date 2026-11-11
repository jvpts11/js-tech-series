---
section: hardware
order: 23
icon: jsc:disk_vaultis_trench_20m
---
# Corruption and the Defrag

Storage is never corrupted in normal play. Two things corrupt it: a network conflict, two Mainframes on one network ([](jsc:network_identity)), which corrupts a tenth of what both networks store; and a thermal trip ([](jsc:cooling)), which may corrupt the disks of the computer that shut down.

> [!DEFINE] Corrupted item
> An item a disk still holds but nobody can read: it leaves the network's index until the Defrag gives it back or loses it.

> [!DEFINE] Bad sectors
> The part of a disk that heavy corruption ruined: the disk holds that much less.

Corrupted items still take their room on the disk, and This PC and the Storage Insights show how much of each disk is corrupted, in red. Corruption can also hit a system's own files, and a system missing the file that starts it does not start. The private part of a Personal Computer is never touched.

Table: How hurt a disk gets

| Corrupted | What it does |
| --- | --- |
| Up to half | Only the items, waiting for the Defrag |
| Over half | Bad sectors take what goes over half: at 70%, the disk holds a fifth less |
| Over nine tenths | The disk dies, with all it held |

Table: The Defrag on each system

| System | Program | How to start it |
| --- | --- | --- |
| MC-DOS and MC-NET | `DEFRAG` | Type defrag at the prompt, then press Enter |
| Frames 95, XP and 7 | Disk Defragmenter | Open it from the Start menu |
| Frames 10 and 11 | Optimize Drives | Open it from the Start menu |
| Linux, FreeBSD and UNIX | `fsck` | Type fsck -y and the disk's device, such as /dev/sdb1 |

> [!DEFINE] Disk Defragmenter
> The Defrag of Frames 95, XP and 7; Frames 10 and 11 call it Optimize Drives.

When you run the Defrag, a pass tries every corrupted item once. What comes back is part of your disk again; what does not is lost. Every pass also mends a quarter of the bad sectors left. While a pass runs, the computer keeps working, but that disk answers the network twice as slowly.

Table: A pass on each kind of disk

| Disk | Chance an item comes back | A pass takes |
| --- | --- | --- |
| Hard disk | 95% | 3 minutes |
| SSD | 70%, 10 less for every tenth corrupted; none from 70% | 1 minute |
| NVMe | 60%, 15 less for every tenth corrupted; none from 50% | 30 seconds |

Frames 7, 10 and 11 run the Defrag on their own once a week on a disk that needs it; its Configure schedule or Change settings button turns that on or off. A system that no longer starts is mended by taking its disk to a Dock Station on another computer and running the Defrag there, or by installing the system again.

The server's settings decide what a failing network costs, in jscomputers-server.toml under [item_loss]: corruption turns all corruption off, network_conflict what a conflict costs, and network_split whether an Operation loses what it carried when the network splits under it, a cable pulled between the Mainframe and a server.

## If something goes wrong

> [!PROBLEM] Items are missing from the network after two Mainframes met on it.
> The conflict corrupted them. Separate the Mainframes, then run the Defrag on each disk.

> [!PROBLEM] The Defrag gave nothing back on an NVMe disk.
> Over half of it was corrupted, and NVMe gives nothing back from there. A hard disk would.

> [!PROBLEM] This PC says a disk has failed.
> Over nine tenths of it was corrupted, and it died with all it held. Replace it.

> [!SEE] jsc:disks, jsc:network_identity, jsc:cooling, jsc:storage_insights

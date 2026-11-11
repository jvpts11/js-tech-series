---
section: library_readings
order: 458
---
# DiskHealth: what happened to the disks

A program can read the health of its machine's drives as the Defrag reads it: whether a disk died, how damaged it is, its bad sectors, the items corruption holds, and when a Defrag pass last ended on it. You read it through DiskHealth, a built-in type of System.Machine. DiskHealth came with Sigma# 3.

Table: DiskHealth members

| Member | What it gives |
| --- | --- |
| `DiskHealth.Of(drive)` | The health of the disk in that drive, named by its letter such as C, or null when no disk is there. |
| `DiskHealth.All()` | The health of every drive with a disk in it, in letter order. |
| `DiskHealth.Drive` | The drive's letter. |
| `DiskHealth.Dead` | Yes when the disk died and holds nothing any more. |
| `DiskHealth.DamagePercent` | How damaged the disk is, from 0 to 100. |
| `DiskHealth.BadSectorsPercent` | How much of the disk bad sectors have taken, from 0 to 100. |
| `DiskHealth.CorruptedItems` | How many items and files corruption holds on the disk. |
| `DiskHealth.LastDefragTick` | The world tick a Defrag pass last ended on the disk, which you compare with Time.Tick, or -1 when none has. |

```sgs Health.sgs
foreach (DiskHealth disk in DiskHealth.All()) {
    if (disk.CorruptedItems > 0) {
        Console.PrintLine(disk.Drive + ": run the Defrag");
    }
}
```

> [!DEFINE] Bad sector
> A part of a disk that no longer holds what is written to it. A Defrag pass mends some of them.

## If something goes wrong

> [!PROBLEM] DiskHealth.Of gives null.
> No disk is in that drive. Check the letter with Computer.Disks().

> [!PROBLEM] DiskHealth.LastDefragTick gives -1.
> No Defrag pass has ended on that disk on this machine.

> [!SEE] jsc:corruption_and_the_defrag, jsc:disks, jsc:computer_info

---
section: network_operation
order: 108
icon: jsc:tape_library
covers_family: jsc:backups
---
# Backups and the Tape Library

The Backup Service backs the network up and puts it back. It runs on a server in a rack, like the Messenger Service, and the Backup Console is its window on any desktop computer: the service keeps the jobs and the backups, the console sets them and restores them.

> [!DEFINE] Backup Service
> The service on a rack server that backs the network up and restores it.

> [!DEFINE] Backup Console
> The desktop window of the Backup Service, in four tabs: Jobs, Back up, Restore and Tapes.

Table: What a backup keeps

| Part | What it is |
| --- | --- |
| Items | What the network's storage holds. |
| Files | The files in every computer's home folder. |
| Settings | Every computer's settings. |
| .craft patterns | The recipes of the network's Crafting Computers. |
| IQL and Sigma# scripts | The scripts on every computer, and the Mainframe's saved views, procedures and jobs. |
| System images | Every computer's system, which puts a corrupted one right. |

## Where it goes

A job writes its backups to a Tape Library or to a drive of another server. On a drive a backup is one file, in the drive's Backups folder, that weighs what the backup weighs. A Tape Library is cold storage, huge and slow: a tape winds to the end of what is written before it writes, and to the backup before it reads, which takes a while.

Table: The tapes

| Tape | Era | Holds |
| --- | --- | --- |
| 9-track reel | Vintage | 150 items, 150 MB |
| DLT | Legacy | 2,560 items, 40 GB |
| LTO-3 | Transition | 1,600 items, 400 GB |
| LTO-6 | Standard | 10,240 items, 2.5 TB |
| LTO-9 | Advanced | 73,728 items, 18 TB |

On a tape, an item weighs what it weighs on a disk of the tape's era. A library takes only the tapes of its own era, and serves the network of the servers in its rack.

## When it runs

A job runs by hand, with Run now in the Jobs tab, or at an hour you give it: every day, or one day of the week. A timed job is an Automation Engine job as well, which runs BACKUP and the job's name at that hour ([](jsc:automation_engine)), so the network needs the Automation Engine on its Mainframe for it.

## Restoring

The Restore tab lists every backup the network holds, the newest first. Choose one, then Restore. A backup on a drive comes back at once; one on a tape waits while the tape winds to it, and the bar shows how long.

Items come back only when they stopped existing. The network keeps a book of losses: what a dead disk took, what a Defrag gave up on, what a RAID 0 lost with a drive, and what a DROP destroyed. Restoring gives back, item by item, the lesser of what the backup holds and what the book lost since the backup was made, and each loss comes back once, whatever backup you restore it from. What was taken out, exported or crafted still exists, so it never comes back: a backup undoes a DROP, not a SELECT.

> [!DEFINE] Book of losses
> The network's record of the items that stopped existing: when, how many, how and where.

Files, settings, patterns and scripts are written back over what is there now. A system image comes back only on the computers you tick, and puts each one's system back as it was, which fixes a corrupted system.

## The tapes

The Tapes tab lists the tapes of a library with their labels, what each holds and when it was last written. Label writes a name on a tape. Erase wipes it, and the backups on it go for good. Eject takes the tape out of its slot, in front of the rack. Neither touches the tape the drive is working on.

## If something goes wrong

> [!PROBLEM] The console says No Backup Service is running on this network.
> Install the Backup Service on a server in a rack of the network, and let the server run.

> [!PROBLEM] A job's last run says no room.
> Its tapes or its drive are full. Put a blank tape in the library, erase one, or choose another drive.

> [!PROBLEM] A job's last run says nowhere to go.
> Its library has no tapes, or its server or drive is gone. Edit the job and choose where it writes.

> [!PROBLEM] A timed job never runs.
> Install the Automation Engine on the Mainframe; the status bar says when there is none.

> [!PROBLEM] A restore gives nothing back.
> Nothing the backup holds was lost since it was made, or a restore already gave it back.

> [!SEE] jsc:network_services, jsc:rack_equipment, jsc:automation_engine

---
section: disks_and_partitions
order: 30
icon: jsc:disk_vaultis_trench_20m
---
# The disk tools

Every system has its own tools to make, format, resize and delete partitions. They all read the same disks, so they never disagree about them.

Table: The tools by system

| System | In a window | At the prompt |
| --- | --- | --- |
| MC-DOS | \- | `FDISK` |
| Frames XP to 11 | Disk Management | `diskpart` |
| Linux with KDE | KDE Partition Manager | `fdisk -l, lsblk, mkfs` |
| Other Linux desktops | Disks | `fdisk -l, lsblk, mkfs` |
| UNIX System V | \- | `fdisk` |
| FreeBSD | \- | `gpart show` |
| OpenBSD | \- | `fdisk, disklabel -h` |

Disk Management shows each volume in a list and each disk as a bar. Right-click a volume to format, shrink, extend or delete it, and free space to make a new one. On Frames XP it cannot resize a volume. The KDE Partition Manager keeps your changes as pending operations, and writes them only when you press Apply. Disks shows one disk at a time, with its volumes as a bar.

## What every tool refuses

The partition the running system runs from, and a swap it swaps to, are never deleted or formatted. A partition never shrinks below what it holds, and each file system resizes by its own rules, in the table on [](jsc:partitions): an ext4 partition in use only grows. A disk changes from MBR to GPT, or back, only when it is empty, and a new partition table erases the whole disk.

1. Open Disk Management, or the tool of your system.
2. Right-click the volume, and choose Shrink Volume.
3. Type how many megabytes to free, and press Shrink.
4. Right-click the free space, and make a new volume in it.

## At the prompt

FDISK on MC-DOS and fdisk on UNIX open a menu: make a partition, set the active one, delete one, or show them. diskpart on Frames takes commands at its own prompt: "list disk", "select disk 1", then "create partition primary", "format fs=ntfs quick", and "exit". On Linux, mkfs.ext4 /dev/sdb1 formats one partition, and refuses the one the system runs from.

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "mkfs.ext4 /dev/sda1", "typed": true}, {"text": "/dev/sda1 is mounted; will not make a filesystem here!"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

## If something goes wrong

> [!PROBLEM] The tool says the volume is in use.
> It is the system's own partition, or its swap. Change it from a live medium or another system.

> [!PROBLEM] Convert to GPT is greyed out.
> The disk still has partitions. Delete them first, or write a new table, which erases the disk.

```command
id: jsc:command/frames/diskpart
```

```command
id: jsc:command/mc_dos/fdisk
```

```command
id: jsc:command/linux/fdisk
```

```command
id: jsc:command/linux/lsblk
```

```command
id: jsc:command/linux/mkfs.ext4
```

```command
id: jsc:command/unix/fdisk
```

```command
id: jsc:command/freebsd/gpart
```

```command
id: jsc:command/openbsd/disklabel
```

> [!SEE] jsc:partitions, jsc:two_systems_on_a_disk

---
section: disk_tools
order: 304
---
# Seeing the disks (lsblk)

Before you change anything on a disk, look at it. The command lsblk lists every disk the computer has, and every partition on those disks, in one table. It answers at once and changes nothing.

You use it at three points: first to find your disk, then after you have divided it, and then after you have made the file systems, to check that each step did what you meant. lsblk needs no words after its name.

Table: The columns of lsblk

| Column | What it tells you |
| --- | --- |
| `NAME` | The name of the device, such as sda for a disk or sda2 for its second partition. |
| `MAJ:MIN` | Two numbers the kernel uses to tell devices apart, which you can ignore. |
| `RM` | Whether the device can be removed; every row here shows 0. |
| `SIZE` | How big the device is: 512M is 512 megabytes, 1G is 1 gigabyte. |
| `RO` | 1 when the device is read-only: the live medium is, and your disks are not. |
| `TYPE` | disk for a whole disk, part for a partition, and loop for the live medium. |
| `MOUNTPOINTS` | Where the device is mounted, or none when it is not. |

Sizes are written with M for megabytes, G for gigabytes and T for terabytes. A decimal place appears only when it is needed: 512M, 1G, 1.5G, 4T.

## Reading the names

A disk takes its name from the slot it sits in, not from how many disks you have. The first slot is sda, the second sdb, and so on. An empty slot still takes its letter, so a disk alone in the third slot is sdc. The computer's own slots come first, then the bay on the front of the case ([](jsc:case_front)).

Read the name from lsblk and use that exact name in every later command. Your disk may not be sda.

## A disk with partitions

A partition is a part of a disk that holds its own file system. lsblk lists each partition under its disk. A vertical bar and a dash mark every partition except the last, and the last one has a back-quote and a dash instead. A disk nobody has divided shows no partition rows at all.

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "lsblk", "typed": true}, {"text": "NAME      MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS"}, {"text": "loop0     7:0      0  1126M  1 loop /run/archiso/airootfs"}, {"text": "sda       8:0      0     1G  0 disk"}, {"text": "|-sda1    8:1      0   512M  0 part /mnt/boot"}, {"text": "`-sda2    8:2      0   512M  0 part /mnt"}], "look": "jsc:guide/terminal/bash", "title": "Arch"}
```

Here your disk is sda, 1G in size, and it is divided into two partitions. You made them with fdisk in the next entry. The MOUNTPOINTS column shows where the new system is attached: sda2 is the root filesystem, mounted at /mnt, and sda1 is mounted at /mnt/boot. The loop0 row is the live medium you are working from, read-only, and it is not a disk you install onto.

> [!DEFINE] Device
> A disk or a partition the system can read and write, named as lsblk prints it.

> [!DEFINE] Partition table
> The list kept at the start of a disk that says where each partition begins and ends.

> [!DEFINE] File system
> The way a partition stores its files: a table of names, folders and data. Each partition holds one, and a tool named mkfs makes an empty one.

> [!DEFINE] Mount
> To attach a file system to a folder, so that its files appear inside that folder.

> [!DEFINE] Mount point
> The folder that a file system is attached to.

> [!DEFINE] Root filesystem
> The file system the whole new system lives in. The system sees it as its top folder, /.

## If something goes wrong

> [!PROBLEM] Your disk is not in the lsblk table.
> Only a disk in one of the computer's disk slots, or in its hot-swap bay, is a device. Put the disk there and run lsblk again.

> [!PROBLEM] The disk has a different name from the one you expected.
> The letter follows the slot the disk sits in. Use the name lsblk prints for it in every later command.

> [!SEE] jsc:disks, jsc:case_front, jsc:server_racks

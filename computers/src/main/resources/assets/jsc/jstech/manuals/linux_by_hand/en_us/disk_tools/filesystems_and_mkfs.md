---
section: disk_tools
order: 306
---
# Giving a partition a file system

A partition is an empty space until it holds a file system, which decides how its files are stored. The new system's root goes on an ext4 file system (the fourth extended file system), and the EFI system partition gets a FAT (File Allocation Table) file system.

Each command below makes one file system, and it erases what was on the partition before. Make a file system only on the partition you have divided for it.

## The root: mkfs.ext4

ext4 is the file system the Linux systems keep their root on. The command mkfs.ext4 makes it, with the name of the partition after it: mkfs.ext4 /dev/sda2.

The partition you name becomes the root. There is only one root at a time: a second mkfs.ext4 on another partition makes that one the root instead. A root that was mounted is unmounted by the new format.

Making the file system takes time, which depends on the disk. A hard disk takes 140 ticks, which is 7 seconds. A solid-state drive (SSD) takes 35 ticks, and an NVMe (non-volatile memory express) disk takes 24 ticks, the least it can take. 20 ticks make one second ([](jsc:units_and_ticks)).

The tool prints these lines as it works, in this order:

Table: What mkfs.ext4 prints

| Line | What it tells you |
| --- | --- |
| `mke2fs 1.47.1 (20-May-2024)` | The version of mke2fs, the program behind mkfs.ext4 that makes ext4. |
| `Creating filesystem with N 4k blocks and M inodes` | How many blocks of 4 kibibytes (KiB) and how many inodes the file system has, which depend on its size. |
| `Filesystem UUID: followed by an identifier` | The identifier of this file system, a universally unique identifier (UUID), the same every time for that disk. |
| `Superblock backups stored on blocks:` | Printed only for a file system bigger than 128 megabytes (MB): the numbers of the blocks that hold spare copies of its header. |
| `Allocating group tables:` | A counting bar, which ends with done. |
| `Writing inode tables:` | A counting bar, which ends with done. |
| `Creating journal (N blocks):` | Printed when the file system has a journal, and then done. |
| `Writing superblocks and filesystem accounting information:` | The last step, a counting bar, which ends with done. |

On a whole disk that has no partition table, the command also works. This is the way of the older machines, which start from the whole disk and have no EFI system partition ([](jsc:eras)).

## The EFI system partition: mkfs.fat

Type mkfs.fat -F 32 /dev/sda1, with the name of the EFI system partition. The tool does not check the -F 32, so type it anyway, as shown.

The tool waits a short while, then prints one line and nothing else: "mkfs.fat 4.2 (2021-01-31)". It takes 17 ticks on a hard disk and 8 ticks on an SSD or NVMe disk. The partition is formatted and not mounted, so you mount it yourself later.

> [!DEFINE] ext4
> The Linux file system that the root filesystem uses. The program mke2fs makes it.

> [!DEFINE] FAT
> The simple file system that the firmware reads from the EFI system partition. It is also called FAT32 or vfat.

> [!DEFINE] Block
> The unit that ext4 stores data in: 4 KiB, which is 4,096 bytes.

> [!DEFINE] Inode
> One record for each file or folder, which says what it is and where its data is. Its number is fixed when the file system is made.

> [!DEFINE] Superblock
> The header of a file system, with its size and layout. Backups are spare copies of it.

> [!DEFINE] Journal
> A small area where ext4 writes each change before it makes the change final, so that a crash does not damage the file system.

> [!DEFINE] UUID
> A universally unique identifier, a long number that names one file system. The new system's file table, /etc/fstab, can name a file system by its UUID, so the device name of the disk does not matter.

## If something goes wrong

> [!PROBLEM] Usage: mkfs.ext4 /dev/<device>
> You did not name a partition. Type mkfs.ext4 /dev/sda2, with the name lsblk gave it.

> [!PROBLEM] mke2fs: No such file or directory while trying to determine filesystem size
> That partition does not exist yet. Make it with fdisk first, or check its number in lsblk.

> [!PROBLEM] mke2fs: sda1 is the EFI System partition.
> That partition holds the boot loader, so it is not for ext4. Format it with mkfs.fat -F 32 /dev/sda1, and give mkfs.ext4 the other partition.

> [!PROBLEM] /dev/sda contains a gpt partition table.
> You named the whole disk, and it has a partition table. Use one of its partitions, such as /dev/sda2.

> [!PROBLEM] Usage: mkfs.fat -F 32 /dev/<partition>
> You did not name a partition. Type mkfs.fat -F 32 /dev/sda1, with the name lsblk gave it.

> [!PROBLEM] mkfs.fat: unable to open sda1: No such file or directory
> That partition does not exist. Make it with fdisk first, or check its number in lsblk.

> [!PROBLEM] mkfs.fat: sda1 is not an EFI System partition.
> Set its type first: fdisk /dev/sda, then type t, then type 1 for EFI System, then type w. Then run mkfs.fat again.

> [!SEE] jsc:firmware, jsc:disks, jsc:units_and_ticks

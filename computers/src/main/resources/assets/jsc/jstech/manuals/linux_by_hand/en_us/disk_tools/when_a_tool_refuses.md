---
section: disk_tools
order: 308
---
# What each disk error means

Most messages start with the tool's name, such as fdisk, mkfs.ext4 or mount. When a tool stops you, find its message in the tables below. A message in a plain sentence, such as Device does not contain a recognized partition table, is information rather than a refusal.

lsblk never refuses anything. It always prints its table.

Table: The messages of fdisk

| Message | What it means, and the fix |
| --- | --- |
| `fdisk: bad usage` | No disk was named: type fdisk /dev/sda. |
| `fdisk: cannot open /dev/sda2: No such file or directory` | No disk of that name is in the computer: use a disk name from lsblk, not a partition name. |
| `Device does not contain a recognized partition table.` | Information: the disk has no partitions yet, so type g for a GPT. |
| `No partition is defined yet!` | t or d needs a partition: make one with n first. |
| `No free sectors available.` | The disk is full, an earlier partition took the rest, or there are 128 partitions: delete the last one with d. |
| `Value out of range.` | The size or number is outside what the disk has: use a smaller size or a number from the list. |
| `Failed to parse partition type 'swap'.` | The type word is not known: type 1 for EFI System, 20 for Linux filesystem, or L for the list. |
| `x: unknown command` | The letter x is not a command of the editor: type m for the list. |
| `Changed type of partition 'Linux filesystem' to 'EFI System'.` | Success, with nothing to fix. |

Table: The messages of mkfs.ext4

| Message | What it means, and the fix |
| --- | --- |
| `Usage: mkfs.ext4 /dev/<device>` | No partition was named: give one, such as /dev/sda2. |
| `mke2fs: No such file or directory while trying to determine filesystem size` | The partition does not exist yet: make it with fdisk, or check its number. |
| `mke2fs: sda1 is the EFI System partition.` | The EFI system partition was named: use mkfs.fat -F 32 on it, and mkfs.ext4 on another partition. |
| `/dev/sda contains a gpt partition table.` | The whole disk was named, and it has a partition table: use one partition, such as sda2. |

Table: The messages of mkfs.fat

| Message | What it means, and the fix |
| --- | --- |
| `Usage: mkfs.fat -F 32 /dev/<partition>` | No partition was named: give one, such as /dev/sda1. |
| `mkfs.fat: unable to open sda1: No such file or directory` | The partition does not exist: make it with fdisk, or check its number. |
| `mkfs.fat: sda1 is not an EFI System partition.` | Its type is not EFI System yet: run fdisk, then t, then 1, then w, and try again. |

Table: The messages of mount

| Message | What it means, and the fix |
| --- | --- |
| `mount: bad usage` | Fewer than two words were given: give a partition and a folder. |
| `mount: /mnt/boot: mount point does not exist.` | The root is not mounted yet, so mount it first. |
| `mount: /mnt/data: mount point does not exist.` | The folder is not a place that can be mounted: use /mnt, or the boot or efi folder under it. |
| `mount: /mnt/boot: wrong fs type on /dev/sda1.` | The EFI system partition is not formatted as FAT, so run mkfs.fat -F 32 on it first. |
| `mount: /mnt: wrong fs type, bad option, bad superblock on /dev/sda2.` | The partition is not the one mkfs.ext4 last formatted, so run mkfs.ext4 on it first. |

A mount or a bind that works prints nothing at all, so silence is its success.

## If something goes wrong

> [!PROBLEM] The same message comes back after you fix something.
> Run lsblk and compare the name you typed with the name it prints. The name follows the slot the disk is in.

> [!PROBLEM] The message is not in these tables.
> Find the tool's name at the start of the message, read that tool's table, and check the name with lsblk.

> [!SEE] jsc:seeing_the_disks, jsc:partitioning_with_fdisk, jsc:mounting

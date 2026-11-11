---
section: disk_tools
order: 305
---
# Dividing a disk (fdisk)

fdisk is the editor that writes a disk's partition table: the list of where each partition begins and ends. You open it on a whole disk, and it asks you what to do next.

The editor works in memory. Nothing reaches the disk until you type w, so you can try things and throw them away. Leaving with q, or with [[Ctrl]]+[[C]], drops everything you typed since the editor opened.

> [!WARNING] Writing with w replaces the disk's partition table, and the partitions it held before are gone.

Table: The commands of fdisk

| Letter | What it does |
| --- | --- |
| `m` | Prints the menu of commands; help does the same. |
| `g` | Makes a new, empty GUID Partition Table (GPT), and removes the partitions shown. |
| `n` | Adds a partition, by a size or by three questions. |
| `t` | Changes the type of a partition. |
| `d` | Deletes a partition. |
| `p` | Prints the table: the disk, its label and its partitions. |
| `w` | Writes the table to the disk and leaves the editor. |
| `q` | Leaves the editor without writing anything. |

## Making a new table (g)

A new disk has no partition table, and fdisk says so when you open it. Type g to make a GPT, the modern kind that UEFI (Unified Extensible Firmware Interface) machines read. The old kind, which p prints as dos, is not one this editor can make.

The line that g prints shows the disk's GUID, a long identifier that stays the same for that disk. Only g makes a new GPT, although a disk that already has partitions may be GPT as well.

## Making a partition (n)

With a size on the line, n makes the partition at once. Type n +512M for 512 megabytes, or n +1G for a gigabyte. A plain number, such as "n 1024", also means megabytes.

Typed alone, n asks three questions in turn. For the first two, press [[Enter]]: the editor picks the number and the starting sector for you. The third asks where the partition ends, and the question shows the range. On a 1G disk it reads:

"Last sector, +/-sectors or +/-size{K,M,G,T,P} (2048-2097118, default 2095103):"

Press [[Enter]] to take the rest of the disk, or type a size such as +512M. A sector is 512 bytes, and a megabyte is 2,048 sectors, so the question also takes a sector number.

When the partition is made, the editor says so: "Created a new partition 1 of type 'Linux filesystem' and of size 512 MiB." Every new partition starts as a Linux filesystem. You change it with t, when it is to be the EFI (Extensible Firmware Interface) system partition.

## Choosing the type (t)

Table: The words that set a type

| Type | Words you can type |
| --- | --- |
| EFI System | `1, uefi, efi, ef` |
| Linux filesystem | `20, linux, 83` |

Type L to see the types the editor knows. It prints two rows, EFI System and Linux filesystem, and then asks again. With one partition on the disk, t selects it and asks for the type. With several, it asks which partition first, and Enter picks the last one.

When the change works, the editor prints the old and new type, exactly as it writes them: "Changed type of partition 'Linux filesystem' to 'EFI System'."

## Printing, deleting and writing (p, d, w)

p prints the table as the editor sees it, before anything is written. It starts with the disk's size, and then one row for each partition. The rows show Device, Start, End, Sectors, Size and Type, in that order.

d deletes a partition and renumbers the rest from partition 1. Then the editor prints "Partition 2 has been deleted.", with the number of the one you chose.

w stores the table on the disk. The editor then says that the partition table has been altered, and that it is syncing the disks. After that the partitions show in lsblk, and mkfs can use them.

## The steps for a UEFI disk

1. Type fdisk /dev/sda and press [[Enter]].
2. Type g and press [[Enter]], to make a GPT.
3. Type n +512M and press [[Enter]], for the first partition.
4. Type t and press [[Enter]], then type 1 for the EFI system partition.
5. Type n and press [[Enter]] three times, to make the second partition from the rest of the disk.
6. Type w and press [[Enter]], to write the table and leave.

> [!DEFINE] GPT
> The modern kind of partition table, which UEFI machines read.

> [!DEFINE] GUID
> A globally unique identifier: a long code, which looks random, that a partition table or a partition is given so that it can be told apart from others.

> [!DEFINE] Sector
> The smallest piece of a disk that the table counts in: 512 bytes.

> [!DEFINE] EFI system partition
> A small partition that the firmware reads to find the boot loader. Only UEFI machines need one.

## If something goes wrong

> [!PROBLEM] fdisk: bad usage
> You did not name a disk. Type fdisk /dev/sda, with the name lsblk gave your disk.

> [!PROBLEM] fdisk: cannot open /dev/sda2: No such file or directory
> fdisk opens a whole disk only, not a partition. Use the disk's name, such as sda.

> [!PROBLEM] Device does not contain a recognized partition table.
> This is information, not a refusal: the disk has no partitions yet. Type g to make a GPT, then n.

> [!PROBLEM] No partition is defined yet!
> t and d need a partition to act on. Make one with n first.

> [!PROBLEM] Value out of range.
> The size or number is too big or too small for this disk. Use a smaller size, or a number from the list p prints.

> [!PROBLEM] No free sectors available.
> The disk is full, or an earlier partition took the rest of it, or there are 128 partitions. Delete the last partition with d. A partition that took the rest has to be deleted and made again with a size.

> [!PROBLEM] Failed to parse partition type 'swap'.
> Type 1 or uefi for an EFI system partition, 20 or linux for a Linux filesystem, or L to see the list.

> [!PROBLEM] x: unknown command
> The letter x is not a command of the editor. Type m to see the commands it has.

> [!PROBLEM] mkfs.fat: sda1 is not an EFI System partition.
> The partition's type was never set to EFI. Run fdisk /dev/sda, then type t, then type 1 for EFI System, then type w.

> [!PROBLEM] You quit with q or [[Ctrl]]+[[C]], and your changes are gone.
> Both leave without writing. Only w keeps the changes, so type w before you leave.

> [!SEE] jsc:firmware, jsc:eras, jsc:arch_bootloader

---
section: disks_and_partitions
order: 29
icon: jsc:disk_vaultis_trench_20m
---
# Partitions and file systems

A disk can be divided into partitions. Each partition is a part of the disk with a file system of its own, and each one that holds a file system is a volume of its own: Frames gives it a drive letter, Linux a name such as /dev/sdb1, and the network keeps items on it as on a disk.

A disk nobody has divided reads as one partition over all of it. Installing a system, or a disk tool, writes the table that lists the partitions.

> [!DEFINE] Partition table
> The list at the start of a disk that says where each partition begins and ends.

Table: The two kinds of table

| Table | Written on | What it holds |
| --- | --- | --- |
| Master Boot Record (MBR) | Vintage to Transition | Four partitions at most, in the first 2 TB of the disk. One of them is active: the one the machine starts. |
| GUID Partition Table (GPT) | Standard and later | As many partitions as the disk has room for, over all of it. |

A machine that starts with UEFI, from the Standard on, reads its systems from an EFI partition: 512 MB of FAT32 that every installer makes first on an empty GPT disk. It holds what starts the systems, and it is never a volume.

> [!DEFINE] EFI partition
> The small FAT32 partition a UEFI machine starts its systems from.

## File systems

Each partition is formatted with one file system. A system reads only the file systems it knows, and a machine makes only those of its era or older.

Table: The file systems

| File system | From | Read by | Resizing |
| --- | --- | --- | --- |
| `FAT12, FAT16` | Vintage | Every system | Unmounted only |
| `FAT32` | Legacy | Every system | Unmounted only |
| `NTFS` | Legacy | Frames; Linux from the Transition | Grows and shrinks while in use |
| `exFAT` | Transition | Frames; Linux from the Advanced | Unmounted only |
| `ext2, ext3` | Legacy | Linux | Grows in use; shrinks unmounted |
| `ext4` | Transition | Linux | Grows in use; shrinks unmounted |
| `XFS` | Transition | Linux | Only grows |
| `btrfs` | Standard | Linux | Grows and shrinks while in use |
| `UFS` | Vintage | UNIX, FreeBSD, OpenBSD | Unmounted only |
| `ZFS` | Transition | FreeBSD | Only grows |

A FAT16 partition holds 2 GB at most, and a file on FAT holds 4 GB at most. btrfs and ZFS keep snapshots ([](jsc:snapshots)), and ZFS mirrors a pool over two disks ([](jsc:zfs_mirrors)). A swap partition holds no file system: it is where the memory overflows to ([](jsc:swap)).

## If something goes wrong

> [!PROBLEM] A partition does not show up as a drive.
> The running system does not read its file system. Format it with one the system reads.

> [!PROBLEM] The tool cannot make the file system you picked.
> It is of a later era than the machine.

> [!SEE] jsc:disks, jsc:disk_tools_of_each_system, jsc:local_storage

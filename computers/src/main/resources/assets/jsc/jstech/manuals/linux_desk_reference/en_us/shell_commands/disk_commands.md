---
section: shell_commands
order: 256
---
# Disks, swap and snapshots

These commands read the disks and their partitions, give a partition a file system, show the swap, and keep snapshots. Each system has its own, which you type at its prompt: what a partition and a file system are is on [](jsc:partitions).

## Linux

fdisk -l prints each disk's partition table, and lsblk lists the disks with their partitions in a tree; lsblk -f adds each one's file system. Each mkfs formats one partition with the file system in its name, and refuses the one the system runs from. To change the partitions themselves, use the desktop's KDE Partition Manager or Disks.

```command
id: jsc:command/linux/fdisk
```

```command
id: jsc:command/linux/lsblk
```

```command
id: jsc:command/linux/mkfs.ext3
```

```command
id: jsc:command/linux/mkfs.ext2
```

```command
id: jsc:command/linux/mkfs.xfs
```

```command
id: jsc:command/linux/mkfs.btrfs
```

```command
id: jsc:command/linux/mkfs.vfat
```

```command
id: jsc:command/linux/mkfs.exfat
```

```command
id: jsc:command/linux/mkfs.ntfs
```

free -h shows the memory and the swap, swapon --show the swap partitions, and top what is running with what of it is in the swap ([](jsc:swap)). btrfs takes and keeps a btrfs partition's snapshots ([](jsc:snapshots)).

```command
id: jsc:command/linux/free
```

```command
id: jsc:command/linux/swapon
```

```command
id: jsc:command/linux/top
```

```command
id: jsc:command/linux/btrfs
```

## FreeBSD

gpart show prints the partition tables. zfs takes, lists and rolls back the snapshots of a ZFS pool, and zpool scrubs a pool and shows what its mirror repaired ([](jsc:zfs_mirrors)). swapinfo and sysctl vm.swap_total show the swap.

```command
id: jsc:command/freebsd/gpart
```

```command
id: jsc:command/freebsd/zfs
```

```command
id: jsc:command/freebsd/zpool
```

```command
id: jsc:command/freebsd/swapinfo
```

```command
id: jsc:command/freebsd/sysctl
```

## OpenBSD and UNIX

On OpenBSD, fdisk prints the Master Boot Record and disklabel -h the system's own partitions inside it. On UNIX System V, fdisk opens a menu that makes and deletes partitions and sets the active one, the one the machine starts.

```command
id: jsc:command/openbsd/fdisk
```

```command
id: jsc:command/openbsd/disklabel
```

```command
id: jsc:command/unix/fdisk
```

## If something goes wrong

> [!PROBLEM] mkfs says the partition is mounted.
> It is the one the system runs from. Format another, or work from a live medium.

> [!PROBLEM] zfs says the dataset does not exist.
> Name the pool as zpool status shows it: zroot for the system's own.

> [!SEE] jsc:partitions, jsc:disk_tools_of_each_system, jsc:file_commands

---
section: disks_and_partitions
order: 32
icon: jsc:disk_vaultis_trench_20m
---
# Snapshots

btrfs and ZFS can keep a snapshot of a partition: its items and its files exactly as they are. Rolling back returns the partition there, which undoes a corruption and anything you lost since.

A snapshot is taken at once and weighs nothing. As the partition changes, it weighs what changed since it was taken, and that takes room on the disk like any items do. Delete a snapshot you no longer need, to give the room back.

> [!DEFINE] Snapshot
> A partition kept as it was at one moment, sharing everything that did not change since.

## btrfs, on Linux

btrfs subvolume snapshot takes one under a path you name. The rollback waits for the next start: set-default picks the snapshot, and the machine mounts it as / when it starts again.

```session
{"lines": [{"prompt": "[player@fedora ~]$ ", "text": "btrfs subvolume snapshot / /.snapshots/day4", "typed": true}, {"text": "Create a snapshot of '/' in '/.snapshots/day4'"}, {"prompt": "[player@fedora ~]$ ", "text": "btrfs subvolume list /", "typed": true}, {"text": "ID 256 gen 1842 top level 5 path root"}, {"text": "ID 257 gen 1842 top level 5 path home"}, {"text": "ID 258 gen 1842 top level 5 path .snapshots/day4"}, {"prompt": "[player@fedora ~]$ ", "text": "btrfs subvolume set-default 258 /", "typed": true}], "look": "jsc:guide/terminal/bash", "title": "Fedora"}
```

btrfs filesystem du -s shows what a snapshot keeps, and in Exclusive what only it keeps: the room that deleting it gives back. Another partition is named by its device, such as /dev/sdb1.

## ZFS, on FreeBSD

zfs snapshot takes one of a pool, named after an at sign, and zfs rollback returns to it at once. While later snapshots are kept, rollback refuses, and -r lets them go.

```session
{"lines": [{"prompt": "root@freebsd:~ # ", "text": "zfs snapshot zroot/usr/home@day4", "typed": true}, {"prompt": "root@freebsd:~ # ", "text": "zfs list -t snapshot", "typed": true}, {"text": "NAME                    USED  AVAIL  REFER  MOUNTPOINT"}, {"text": "zroot/usr/home@day4       0B      -  41.2G  -"}, {"prompt": "root@freebsd:~ # ", "text": "zfs rollback zroot/usr/home@day4", "typed": true}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

The system's own pool is zroot; another is named by its partition's label.

## If something goes wrong

> [!PROBLEM] The disk fills up after a snapshot.
> It keeps what you took out since. Delete the snapshots you do not need.

> [!PROBLEM] The command says the partition is not btrfs.
> Only btrfs and ZFS keep snapshots. Name the partition by its device if it is not the root.

```command
id: jsc:command/linux/btrfs
```

```command
id: jsc:command/freebsd/zfs
```

> [!SEE] jsc:zfs_mirrors, jsc:corruption_and_the_defrag

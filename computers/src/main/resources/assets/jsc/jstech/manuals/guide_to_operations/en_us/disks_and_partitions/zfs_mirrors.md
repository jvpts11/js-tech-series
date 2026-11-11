---
section: disks_and_partitions
order: 33
icon: jsc:disk_vaultis_trench_20m
---
# ZFS mirrors

A ZFS pool can live on two disks at once, each holding the same. When corruption damages part of the pool, ZFS repairs it from the other disk: as soon as that part is read, and when you scrub the pool, which reads all of it.

> [!DEFINE] Mirror
> A pool held whole on two disks, so either repairs the other.

> [!DEFINE] Scrub
> Reading a whole pool to find and repair what is damaged.

FreeBSD's installer makes a mirror: choose Auto (ZFS), then the second disk in the ZFS Configuration step. The second disk is no volume of its own; it is what the pool repairs from.

```session
{"lines": [{"prompt": "root@freebsd:~ # ", "text": "zpool scrub zroot", "typed": true}, {"prompt": "root@freebsd:~ # ", "text": "zpool status zroot", "typed": true}, {"text": "  pool: zroot"}, {"text": " state: ONLINE"}, {"text": "  scan: scrub repaired 8M in 00:00:00 with 0 errors on Day 4 10:12"}, {"text": "config:"}, {"text": ""}, {"text": "        NAME        STATE     READ WRITE CKSUM"}, {"text": "        zroot       ONLINE       0     0     0"}, {"text": "          mirror-0  ONLINE       0     0     0"}, {"text": "            ada0p4  ONLINE       0     0     2  (repaired)"}, {"text": "            ada1p4  ONLINE       0     0     0"}, {"text": ""}, {"text": "errors: No known data errors"}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

CKSUM counts how many times the pool found damage and repaired it. The scan line says what the last scrub repaired, and when.

Without its second disk the pool is DEGRADED: it keeps working, finds the damage, and has nothing to repair it from. The Defrag is then the only cure, or a snapshot.

## If something goes wrong

> [!PROBLEM] zpool status says DEGRADED.
> The second disk is not in the machine, or it died. Put it back.

> [!PROBLEM] errors lists data errors after a scrub.
> The pool has no second disk to repair from. Run the Defrag, or roll back to a snapshot.

```command
id: jsc:command/freebsd/zpool
```

> [!SEE] jsc:snapshots, jsc:corruption_and_the_defrag
